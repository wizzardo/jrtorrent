package com.wizzardo.jrt.bt;

import bt.Bt;
import bt.data.*;
import bt.data.digest.Digester;
import bt.data.file.FileSystemStorage;
import bt.dht.DHTConfig;
import bt.dht.DHTModule;
import bt.event.EventSink;
import bt.event.EventSource;
import bt.metainfo.MetadataService;
import bt.metainfo.Torrent;
import bt.metainfo.TorrentFile;
import bt.metainfo.TorrentId;
import bt.protocol.BitOrder;
import bt.protocol.Protocols;
import bt.runtime.*;
import bt.service.IRuntimeLifecycleBinder;
import bt.torrent.AdhocTorrentRegistry;
import bt.torrent.TorrentDescriptor;
import bt.torrent.TorrentRegistry;
import bt.torrent.data.BlockCache;
import bt.torrent.data.CachedDataWorker;
import bt.torrent.data.DataWorker;
import bt.tracker.http.HttpTrackerModule;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.wizzardo.http.framework.Holders;
import com.wizzardo.http.framework.di.PostConstruct;
import com.wizzardo.http.framework.di.Service;
import com.wizzardo.http.utils.PercentEncoding;
import com.wizzardo.jrt.*;
import com.wizzardo.jrt.db.DBService;
import com.wizzardo.jrt.db.generated.Tables;
import com.wizzardo.jrt.db.model.TorrentBinary;
import com.wizzardo.jrt.db.model.TorrentBitfield;
import com.wizzardo.jrt.db.model.TorrentEntryPriority;
import com.wizzardo.jrt.db.query.QueryBuilder.TIMESTAMP;
import com.wizzardo.tools.io.FileTools;
import com.wizzardo.tools.misc.Pair;
import com.wizzardo.tools.misc.event.EventBus;
import com.wizzardo.tools.security.Base64;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.Security;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.wizzardo.tools.misc.With.with;

public class BtService implements Service, TorrentClientService, PostConstruct {

    DBService dbService;
    AppWebSocketHandler appWebSocketHandler;
    MetadataService metadataService = new MetadataService();
    Map<String, ActiveClient> clients = new ConcurrentHashMap<>();
    public BtRuntime btRuntime;
    AdhocTorrentRegistry torrentRegistry;
    EventSink eventSink;
    EventBus<Events> eventBus = new EventBus<>();
    File downloadsDir;
    volatile boolean broadcasting = false;

    static class ActiveClient {
        final TorrentId torrentId;
        final BtClient client;
        final TorrentInfo torrentInfo;
        final PrioritizedSequentialPieceSelector pieceSelector;
        final AppWebSocketHandler.TorrentUpdated updatedEvent;
        final MovingAverage downloadSpeed = new MovingAverage(5);
        final MovingAverage uploadSpeed = new MovingAverage(5);

        ActiveClient(BtClient client, TorrentInfo torrentInfo, PrioritizedSequentialPieceSelector pieceSelector) {
            this.client = client;
            this.torrentInfo = torrentInfo;
            this.pieceSelector = pieceSelector;
            this.torrentId = TorrentId.fromBytes(Protocols.fromHex(torrentInfo.getHash()));
            updatedEvent = new AppWebSocketHandler.TorrentUpdated();
        }

        public void setPriority(String path, FilePriority priority) {
            pieceSelector.setPriority(path, priority);
        }

    }

    @Override
    public List<TorrentInfo> list() {
        return dbService.withBuilder(b -> {
            return b.select(Tables.TORRENT_INFO.FIELDS).from(Tables.TORRENT_INFO).fetchInto(com.wizzardo.jrt.db.model.TorrentInfo.class);
        }).stream()
                .map(torrentInfo -> with(new TorrentInfo(), ti -> mapToTorrentInfoDTO(torrentInfo, ti)))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<TorrentEntry> entries(TorrentInfo ti) {
        return entries(ti.getHash());
    }

    @Override
    public Collection<TorrentEntry> entries(String hash) {
        TorrentBinary torrentBinary = getTorrentBinary(hash);
        if (torrentBinary == null)
            return Collections.emptyList();
        Torrent torrent = metadataService.fromByteArray(torrentBinary.data);

        List<TorrentEntryPriority> priorities = getTorrentEntriesPriorities(hash);

        TorrentEntry rootEntry;
        ActiveClient ac = clients.get(hash);
        if (ac != null && ac.pieceSelector.filesWithPieces != null) {
            List<PrioritizedSequentialPieceSelector.TorrentFileWithPieces> files = ac.pieceSelector.filesWithPieces;
            rootEntry = createTree(torrent.getName(), files);
        } else {
            Path targetDirectory = getDownloadPath();
            Storage storage = new FileSystemStorage(targetDirectory);
            PrioritizedSequentialPieceSelector pieceSelector = new PrioritizedSequentialPieceSelector(storage);
            pieceSelector.setTorrent(torrent);
            rootEntry = createTree(torrent.getName(), pieceSelector.filesWithPieces);
        }

        setPriorities(rootEntry, priorities);
        if (rootEntry.getChildren().size() == 1) {
            if (rootEntry.getChildren().get(torrent.getName()) != null)
                return rootEntry.getChildren().values();
        }
        return Collections.singletonList(rootEntry);
    }

    static TorrentEntry createTree(String torrentName, List<PrioritizedSequentialPieceSelector.TorrentFileWithPieces> files) {
        int id = 0;
        TorrentEntry rootEntry = new TorrentEntry(torrentName);
        for (PrioritizedSequentialPieceSelector.TorrentFileWithPieces file : files) {
            TorrentEntry entry = rootEntry;
            for (String pathElement : file.getPathElements()) {
                entry = entry.getOrCreate(pathElement);
                if (entry.getId() == -1)
                    entry.setId(id++);
            }
            entry.setSizeBytes(file.getSize());
            entry.setPriority(file.priority);
            entry.setPiecesLength(file.pieces.length);
            entry.setPiecesOffset(file.pieces[0]);
        }
        return rootEntry;
    }

    void setPriorities(TorrentEntry root, List<TorrentEntryPriority> priorities) {
        outer:
        for (TorrentEntryPriority entryPriority : priorities) {
            String[] path = entryPriority.path.split("/");

            TorrentEntry entry = root;
            System.out.println("setting priority " + Arrays.toString(path));
            for (int i = 0; i < path.length; i++) {
                String s = path[i];
                entry = entry.get(s);

                if (entry == null) {
                    System.out.println("entry not found at step " + (i) + ": " + path[i]);
                    continue outer;
                }
            }
            setPriority(entry, entryPriority);
        }
    }

    private void setPriority(TorrentEntry entry, TorrentEntryPriority entryPriority) {
        if (entry.isFolder()) {
            for (TorrentEntry torrentEntry : entry.getChildren().values()) {
                setPriority(torrentEntry, entryPriority);
            }
        }
        entry.setPriority(entryPriority.priority);
    }

    private List<TorrentEntryPriority> getTorrentEntriesPriorities(String hash) {
        return dbService.withBuilder(b -> b.select(Tables.TORRENT_ENTRY_PRIORITY.FIELDS)
                .from(Tables.TORRENT_ENTRY_PRIORITY)
                .join(Tables.TORRENT_INFO).on(Tables.TORRENT_INFO.ID.eq(Tables.TORRENT_ENTRY_PRIORITY.TORRENT_INFO_ID))
                .where(Tables.TORRENT_INFO.HASH.eq(hash))
                .orderBy(Tables.TORRENT_ENTRY_PRIORITY.DATE_CREATED)
                .fetchInto(TorrentEntryPriority.class));
    }

    @Override
    public void load(String torrent) {
    }

    @Override
    public void load(String torrent, boolean autostart) {
        System.out.println("adding new torrent: " + torrent);

        com.wizzardo.jrt.db.model.TorrentInfo torrentInfo;
        if (torrent.startsWith("/")) {
            byte[] bytes = FileTools.bytes(torrent);
            Torrent t = metadataService.fromByteArray(bytes);

            TorrentInfo.Status status = autostart ? TorrentInfo.Status.DOWNLOADING : TorrentInfo.Status.STOPPED;
            torrentInfo = new com.wizzardo.jrt.db.model.TorrentInfo(TIMESTAMP.now(), TIMESTAMP.now(), t.getName(),
                    t.getTorrentId().toString(), t.getSize(), 0, 0, status, 0, 0);

            dbService.withBuilder(b -> {
                torrentInfo.id = dbService.insertInto(b, torrentInfo, Tables.TORRENT_INFO);
                TorrentBinary binary = new TorrentBinary(TIMESTAMP.now(), TIMESTAMP.now(), torrentInfo.id, bytes);
                binary.id = dbService.insertInto(b, binary, Tables.TORRENT_BINARY);
                return binary;
            });

            ActiveClient ac = loadTorrentFile(t, torrentInfo);
            clients.put(t.getTorrentId().toString(), ac);

            if (autostart) {
                startClient(ac);
            }

            appWebSocketHandler.onAdd(with(new TorrentInfo(), ti -> {
                mapToTorrentInfoDTO(torrentInfo, ti);
            }));
        } else {
            Map<String, List<String>> magnet = parseMagnet(torrent);

            String hash = magnet.getOrDefault("xt", Collections.emptyList()).get(0);
            hash = hash.substring("urn:btih:".length()).toLowerCase();

            String name = magnet.getOrDefault("dn", Collections.singletonList(hash)).get(0);
            long size = Long.parseLong(magnet.getOrDefault("xl", Collections.singletonList("0")).get(0));


            TorrentInfo.Status status = autostart ? TorrentInfo.Status.DOWNLOADING : TorrentInfo.Status.STOPPED;
            torrentInfo = new com.wizzardo.jrt.db.model.TorrentInfo(TIMESTAMP.now(), TIMESTAMP.now(), name,
                    hash, size, 0, 0, status, 0, 0);

            ActiveClient ac = loadTorrentFile(torrent, torrentInfo, t -> {
                dbService.withBuilder(b -> {
                    torrentInfo.name = t.getName();
                    torrentInfo.size = t.getSize();
                    torrentInfo.id = dbService.insertInto(b, torrentInfo, Tables.TORRENT_INFO);
                    TorrentBinary binary = new TorrentBinary(TIMESTAMP.now(), TIMESTAMP.now(), torrentInfo.id, t.getSource().getExchangedMetadata());
                    binary.id = dbService.insertInto(b, binary, Tables.TORRENT_BINARY);
                    return binary;
                });
            });
            clients.put(hash, ac);

            if (autostart) {
                startClient(ac);
            }

            appWebSocketHandler.onAdd(with(new TorrentInfo(), ti -> {
                mapToTorrentInfoDTO(torrentInfo, ti);
            }));
        }
    }

    private static Map<String, List<String>> parseMagnet(String magnet) {
        int start = magnet.indexOf("?");
        if (start == -1)
            return Collections.emptyMap();

        start++;
        Map<String, List<String>> map = new HashMap<>();
        do {
            int end = magnet.indexOf("=", start);
            String key = magnet.substring(start, end);

            start = end + 1;
            end = magnet.indexOf("&", start);

            if (end == -1)
                end = magnet.length();

            String value = magnet.substring(start, end);

            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            int l = PercentEncoding.decode(bytes, 0, bytes.length);
            value = new String(bytes, 0, l);

            map.computeIfAbsent(key, s -> new ArrayList<>()).add(value);

            start = end + 1;
        } while (start < magnet.length());
        return map;
    }

    private void startClient(ActiveClient ac) {
        long wasDownloaded = ac.torrentInfo.getDownloaded();
        long wasUploaded = ac.torrentInfo.getUploaded();

        AtomicLong downloaded = new AtomicLong();
        AtomicLong uploaded = new AtomicLong();
        AppWebSocketHandler.toSerializedView(ac.torrentInfo, ac.updatedEvent);

        ac.client.startAsync(state -> {
            if (state.getPiecesTotal() == 1)
                return;

            ac.torrentInfo.setDownloaded(wasDownloaded + state.getDownloaded());
            ac.torrentInfo.setUploaded(wasUploaded + state.getUploaded());

            try {
                ac.downloadSpeed.add(ac.torrentInfo.getDownloaded() - downloaded.get());
                ac.uploadSpeed.add(ac.torrentInfo.getUploaded() - uploaded.get());
                ac.torrentInfo.setDownloadSpeed(ac.downloadSpeed.get());
                ac.torrentInfo.setUploadSpeed(ac.uploadSpeed.get());
            } catch (Exception e) {
                e.printStackTrace();
            }

            downloaded.set(ac.torrentInfo.getDownloaded());
            uploaded.set(ac.torrentInfo.getUploaded());

            int piecesComplete = state.getPiecesComplete();
            if (ac.torrentInfo.getDownloadSpeed() != 0 || ac.torrentInfo.getUploadSpeed() != 0)
                updateTorrent(ac.torrentInfo, piecesComplete, state.getPiecesTotal());

            System.out.println(piecesComplete + "/" + state.getPiecesTotal() + " peers: " + state.getConnectedPeers().size() + "  " + ac.downloadSpeed);
            state.getConnectedPeers().stream()
                    .map(key -> Pair.of(key, state.getPeerConnectionState(key)))
//                    .filter()
                    .sorted(Comparator.comparingLong(o -> o.value.getDownloaded()))
                    .forEach(p -> System.out.println(p.key.getPeer().getInetAddress() + ":" + p.key.getPeer().getPort()
                            + " " + p.key.getPeer().getPeerId().map(peerId -> new String(peerId.getBytes())).orElse("unknown")
                            + " downloaded:" + p.value.getDownloaded() / 1024 / 1024f + "mb"
                            + ", Pending: " + p.value.getPendingRequests().size()
                            + ", Cancelled: " + p.value.getCancelledPeerRequests().size()
                            + ", isChoking: " + p.value.isChoking()
                            + ", isPeerChoking: " + p.value.isPeerChoking()
                            + ", isInterested: " + p.value.isInterested()
                            + ", pieces: " + p.value.getEnqueuedPieces()
                    ));

            if (broadcasting) {
                Optional<TorrentDescriptor> descriptor = torrentRegistry.getDescriptor(ac.torrentId);
                if (descriptor.isPresent()) {
                    Bitfield bitfield = descriptor.get().getDataDescriptor().getBitfield();
                    byte[] bytes = bitfield.toByteArray(BitOrder.BIG_ENDIAN);
                    ac.updatedEvent.bitfield = Base64.encodeToString(bytes);
                    ac.updatedEvent.piecesTotal = bitfield.getPiecesTotal();
                    ac.updatedEvent.piecesComplete = bitfield.getPiecesComplete();
                }

                AppWebSocketHandler.toSerializedView(ac.torrentInfo, ac.updatedEvent);
                ac.updatedEvent.progress = ac.torrentInfo.getSize() == 0 ? 0 : piecesComplete * 100f / state.getPiecesTotal();

                appWebSocketHandler.addBroadcastTask(ac.updatedEvent);
            }
        }, 1000);
    }

    private void mapToTorrentInfoDTO(com.wizzardo.jrt.db.model.TorrentInfo torrentInfo, TorrentInfo ti) {
        ti.setName(torrentInfo.name);
        ti.setHash(torrentInfo.hash);
        ti.setStatus(torrentInfo.status);
        ti.setSize(torrentInfo.size);
        ti.setDownloaded(torrentInfo.downloaded);
        ti.setUploaded(torrentInfo.uploaded);
    }

    @Override
    public String getEncodedBitfield(String hash) {
        ActiveClient client = clients.get(hash);
        if (client != null) {
            Optional<TorrentDescriptor> descriptor = torrentRegistry.getDescriptor(client.torrentId);
            if (descriptor.isPresent()) {
                Bitfield bitfield = descriptor.get().getDataDescriptor().getBitfield();
                byte[] bytes = bitfield.toByteArray(BitOrder.BIG_ENDIAN);
                return Base64.encodeToString(bytes);
            }
        }

        TorrentBitfield torrentBitfield = getTorrentBitfield(hash);
        if (torrentBitfield != null) {
            return Base64.encodeToString(torrentBitfield.data);
        }
        return null;
    }

    protected Config createDefaultConfig() {
        Config config = new Config() {
            @Override
            public int getNumOfHashingThreads() {
//                return Runtime.getRuntime().availableProcessors() * 2;
                return 1;
            }
        };
//        config.setEncryptionPolicy(EncryptionPolicy.REQUIRE_ENCRYPTED);
        config.setNetworkBufferSize(256 * 1024);
        config.setMaxConcurrentlyActivePeerConnectionsPerTorrent(512);
        config.setMaxPieceReceivingTime(Duration.ofSeconds(120));
//        config.setMaxOutstandingRequests(16 * 1024 / 16 + 1);
        config.setMaxOutstandingRequests(128);
//        config.setTransferBlockSize(64 * 1024);
        config.setTransferBlockSize(16 * 1024); // 32kb might be supported but not more >_<
        config.setPeerConnectionInactivityThreshold(Duration.ofSeconds(60));
        config.setPeerHandshakeTimeout(Duration.ofSeconds(5));
        config.setMaxPendingConnectionRequests(1);
        return config;
    }

    protected Module createDHTModule() {
        return new DHTModule(new DHTConfig() {
            @Override
            public boolean shouldUseRouterBootstrap() {
                return true;
            }
        });
    }

    public Path getDownloadPath() {
        return new File(Holders.getConfig().config("jrt").get("downloads", ".")).toPath();
    }

    protected ActiveClient loadTorrentFile(Torrent torrent, com.wizzardo.jrt.db.model.TorrentInfo torrentInfo) {
        Path targetDirectory = getDownloadPath();

        Storage storage = new FileSystemStorage(targetDirectory);

        PrioritizedSequentialPieceSelector pieceSelector = new PrioritizedSequentialPieceSelector(storage);
        BtClient client = Bt.client(btRuntime)
                .storage(storage)
                .torrent(() -> torrent)
//                .sequentialSelector()
                .selector(pieceSelector)
                .afterTorrentFetched(t -> {
                    pieceSelector.setTorrent(torrent);
                    getTorrentEntriesPriorities(torrentInfo.hash).forEach(entryPriority -> pieceSelector.setPriority(entryPriority.path, entryPriority.priority));
                })
                .build();
        return new ActiveClient(client, with(new TorrentInfo(), ti -> mapToTorrentInfoDTO(torrentInfo, ti)), pieceSelector);
    }

    protected ActiveClient loadTorrentFile(String magnet, com.wizzardo.jrt.db.model.TorrentInfo torrentInfo, Consumer<Torrent> torrentConsumer) {
        Path targetDirectory = getDownloadPath();

        Storage storage = new FileSystemStorage(targetDirectory);

        TorrentInfo info = with(new TorrentInfo(), ti -> mapToTorrentInfoDTO(torrentInfo, ti));
        PrioritizedSequentialPieceSelector pieceSelector = new PrioritizedSequentialPieceSelector(storage);
        BtClient client = Bt.client(btRuntime)
                .storage(storage)
                .magnet(magnet)
//                .sequentialSelector()
                .selector(pieceSelector)
                .afterTorrentFetched(torrent -> {
                    pieceSelector.setTorrent(torrent);
                    getTorrentEntriesPriorities(torrentInfo.hash).forEach(entryPriority -> pieceSelector.setPriority(entryPriority.path, entryPriority.priority));
                    torrentConsumer.accept(torrent);
                    info.setSize(torrent.getSize());
                    info.setName(torrent.getName());
                })
                .build();
        return new ActiveClient(client, info, pieceSelector);
    }

    @Override
    public void start(String hash) {
        com.wizzardo.jrt.db.model.TorrentInfo torrentInfo = getTorrentInfo(hash);
        if (torrentInfo == null)
            return;

        torrentInfo.status = TorrentInfo.Status.DOWNLOADING;


        ActiveClient ac = clients.get(hash);
        if (ac == null) {
            TorrentBinary binary = getTorrentBinary(torrentInfo);
            if (binary == null)
                return;

            Torrent torrent = metadataService.fromByteArray(binary.data);

            ac = loadTorrentFile(torrent, torrentInfo);
            clients.put(hash, ac);
        }

        if (!ac.client.isStarted())
            startClient(ac);

        dbService.withBuilder(b -> b
                .update(Tables.TORRENT_INFO)
                .set(Tables.TORRENT_INFO.DATE_UPDATED.eq(TIMESTAMP.now()))
                .set(Tables.TORRENT_INFO.STATUS.eq(torrentInfo.status))
                .where(Tables.TORRENT_INFO.HASH.eq(hash))
                .executeUpdate()
        );
        appWebSocketHandler.onUpdate(with(new TorrentInfo(), ti -> mapToTorrentInfoDTO(torrentInfo, ti)));
    }

    private com.wizzardo.jrt.db.model.TorrentInfo getTorrentInfo(String hash) {
        return dbService.withBuilder(b -> b
                .select(Tables.TORRENT_INFO.FIELDS)
                .from(Tables.TORRENT_INFO)
                .where(Tables.TORRENT_INFO.HASH.eq(hash))
                .fetchOneInto(com.wizzardo.jrt.db.model.TorrentInfo.class)
        );
    }

    @Override
    public void stop(String hash) {
        storeTorrentBitfield(hash);

        ActiveClient ac = clients.remove(hash);
        if (ac != null) {
            ac.client.stop();
            TorrentId torrentId = ac.torrentId;
            torrentRegistry.getDescriptor(torrentId).ifPresent(TorrentDescriptor::stop);
            eventSink.fireTorrentStopped(torrentId);
            torrentRegistry.unregister(torrentId);
        }

        System.out.println("eventBus.trigger Events.STOP_TORRENT " + hash + "  " + eventBus.toString());
        eventBus.trigger(Events.STOP_TORRENT, hash);

        com.wizzardo.jrt.db.model.TorrentInfo torrentInfo = getTorrentInfo(hash);
        if (torrentInfo == null)
            return;

        torrentInfo.status = TorrentInfo.Status.STOPPED;
        if (ac != null) {
            ac.torrentInfo.setStatus(TorrentInfo.Status.STOPPED);
            updateTorrent(ac.torrentInfo);
            torrentInfo.downloaded = ac.torrentInfo.getDownloaded();
            torrentInfo.uploaded = ac.torrentInfo.getUploaded();
        } else
            updateTorrent(torrentInfo);
        appWebSocketHandler.onUpdate(with(new TorrentInfo(), ti -> mapToTorrentInfoDTO(torrentInfo, ti)));
    }

    private void updateTorrent(TorrentInfo torrentInfo) {
        dbService.withBuilder(b -> b
                .update(Tables.TORRENT_INFO)
                .set(Tables.TORRENT_INFO.DATE_UPDATED.eq(TIMESTAMP.now()))
                .set(Tables.TORRENT_INFO.STATUS.eq(torrentInfo.getStatus()))
                .set(Tables.TORRENT_INFO.DOWNLOADED.eq(torrentInfo.getDownloaded()))
                .set(Tables.TORRENT_INFO.UPLOADED.eq(torrentInfo.getUploaded()))
                .where(Tables.TORRENT_INFO.HASH.eq(torrentInfo.getHash()))
                .executeUpdate()
        );
    }

    private void updateTorrent(TorrentInfo torrentInfo, int piecesComplete, int piecesTotal) {
        dbService.withBuilder(b -> b
                .update(Tables.TORRENT_INFO)
                .set(Tables.TORRENT_INFO.DATE_UPDATED.eq(TIMESTAMP.now()))
                .set(Tables.TORRENT_INFO.STATUS.eq(torrentInfo.getStatus()))
                .set(Tables.TORRENT_INFO.DOWNLOADED.eq(torrentInfo.getDownloaded()))
                .set(Tables.TORRENT_INFO.UPLOADED.eq(torrentInfo.getUploaded()))
                .set(Tables.TORRENT_INFO.PIECES_COMPLETE.eq(piecesComplete))
                .set(Tables.TORRENT_INFO.PIECES_COUNT.eq(piecesTotal))
                .where(Tables.TORRENT_INFO.HASH.eq(torrentInfo.getHash()))
                .executeUpdate()
        );
    }

    private void updateTorrent(com.wizzardo.jrt.db.model.TorrentInfo torrentInfo) {
        dbService.withBuilder(b -> b
                .update(Tables.TORRENT_INFO)
                .set(Tables.TORRENT_INFO.DATE_UPDATED.eq(TIMESTAMP.now()))
                .set(Tables.TORRENT_INFO.STATUS.eq(torrentInfo.status))
                .set(Tables.TORRENT_INFO.DOWNLOADED.eq(torrentInfo.downloaded))
                .set(Tables.TORRENT_INFO.UPLOADED.eq(torrentInfo.uploaded))
                .where(Tables.TORRENT_INFO.HASH.eq(torrentInfo.hash))
                .executeUpdate()
        );
    }

    @Override
    public void delete(String hash, boolean withData) {
        System.out.println("delete " + hash);
        com.wizzardo.jrt.db.model.TorrentInfo torrentInfo = getTorrentInfo(hash);

        if (torrentInfo == null) {
            System.out.println("didn't find torrent for hash: " + hash);
            List<com.wizzardo.jrt.db.model.TorrentInfo> infos = dbService.withBuilder(b -> b
                    .select(Tables.TORRENT_INFO.FIELDS)
                    .from(Tables.TORRENT_INFO)
                    .fetchInto(com.wizzardo.jrt.db.model.TorrentInfo.class)
            );
            System.out.println("torrents in db: ");
            for (com.wizzardo.jrt.db.model.TorrentInfo info : infos) {
                System.out.println(info);
            }

            return;
        }

        ActiveClient ac = clients.remove(hash);
        if (ac != null) {
            ac.client.stop();
            TorrentId torrentId = ac.torrentId;
            torrentRegistry.getDescriptor(torrentId).ifPresent(TorrentDescriptor::stop);
            eventSink.fireTorrentStopped(torrentId);
            torrentRegistry.unregister(torrentId);
        }


        System.out.println("deleting " + torrentInfo);
        TorrentBinary torrentBinary = getTorrentBinary(torrentInfo);
        if (withData && torrentBinary != null) {
            Torrent torrent = metadataService.fromByteArray(torrentBinary.data);
            System.out.println("removing " + new File(getDownloadPath().toFile(), torrent.getName()).getAbsolutePath());
            FileTools.deleteRecursive(new File(getDownloadPath().toFile(), torrent.getName()));
        }

        dbService.withBuilder(b -> b.deleteFrom(Tables.TORRENT_INFO).where(Tables.TORRENT_INFO.ID.eq(torrentInfo.id)).executeUpdate());

        TorrentInfo ti = new TorrentInfo();
        mapToTorrentInfoDTO(torrentInfo, ti);
        appWebSocketHandler.onRemove(ti);
    }

    private void storeTorrentBitfield(String hash) {
        ActiveClient client = clients.get(hash);
        if (client == null)
            return;
        Optional<TorrentDescriptor> descriptor = torrentRegistry.getDescriptor(client.torrentId);
        if (!descriptor.isPresent())
            return;

        Bitfield bitfield = descriptor.get().getDataDescriptor().getBitfield();
        byte[] bytes = bitfield.toByteArray(BitOrder.BIG_ENDIAN);

        TorrentBitfield torrentBitfield = getTorrentBitfield(hash);
        if (torrentBitfield != null) {
            if (Arrays.equals(torrentBitfield.data, bytes))
                return;

            dbService.consume(b -> b.update(Tables.TORRENT_BITFIELD)
                    .set(Tables.TORRENT_BITFIELD.DATE_UPDATED.eq(TIMESTAMP.now()))
                    .set(Tables.TORRENT_BITFIELD.DATA.eq(bytes))
                    .where(Tables.TORRENT_BITFIELD.ID.eq(torrentBitfield.id))
                    .executeUpdate());
        } else {
            TorrentBitfield tb = new TorrentBitfield(TIMESTAMP.now(), TIMESTAMP.now(), getTorrentInfo(hash).id, bytes);
            tb.id = dbService.withBuilder(b -> dbService.insertInto(b, tb, Tables.TORRENT_BITFIELD));
        }
    }

    private void resumeDownloading() {
        dbService.withBuilder(b -> b
                .select(Tables.TORRENT_INFO.FIELDS)
                .from(Tables.TORRENT_INFO)
                .where(Tables.TORRENT_INFO.STATUS.eq(TorrentInfo.Status.DOWNLOADING))
                .fetchInto(com.wizzardo.jrt.db.model.TorrentInfo.class)
        ).forEach(torrentInfo -> {
            TorrentBinary binary = getTorrentBinary(torrentInfo);
            if (binary == null)
                return;

            Torrent torrent = metadataService.fromByteArray(binary.data);
            ActiveClient ac = loadTorrentFile(torrent, torrentInfo);
            clients.put(torrentInfo.hash, ac);

            startClient(ac);
        });
    }

    private TorrentBinary getTorrentBinary(com.wizzardo.jrt.db.model.TorrentInfo torrentInfo) {
        return dbService.withBuilder(b -> b.select(Tables.TORRENT_BINARY.FIELDS).from(Tables.TORRENT_BINARY).where(Tables.TORRENT_BINARY.TORRENT_INFO_ID.eq(torrentInfo.id)).fetchOneInto(TorrentBinary.class));
    }

    private TorrentBinary getTorrentBinary(String hash) {
        return dbService.withBuilder(b -> b.select(Tables.TORRENT_BINARY.FIELDS)
                .from(Tables.TORRENT_BINARY)
                .join(Tables.TORRENT_INFO).on(Tables.TORRENT_INFO.ID.eq(Tables.TORRENT_BINARY.TORRENT_INFO_ID))
                .where(Tables.TORRENT_INFO.HASH.eq(hash))
                .fetchOneInto(TorrentBinary.class));
    }

    private TorrentBitfield getTorrentBitfield(String hash) {
        return dbService.withBuilder(b -> b.select(Tables.TORRENT_BITFIELD.FIELDS)
                .from(Tables.TORRENT_BITFIELD)
                .join(Tables.TORRENT_INFO).on(Tables.TORRENT_INFO.ID.eq(Tables.TORRENT_BITFIELD.TORRENT_INFO_ID))
                .where(Tables.TORRENT_INFO.HASH.eq(hash))
                .fetchOneInto(TorrentBitfield.class));
    }

    @Override
    public void setPriority(String hash, String path, FilePriority priority) {
        System.out.println("setPriority for " + hash + " " + path + " to " + priority);
        com.wizzardo.jrt.db.model.TorrentInfo torrentInfo = getTorrentInfo(hash);
        if (torrentInfo == null)
            return;

        byte[] bytes = path.getBytes(StandardCharsets.UTF_8);
        int l = PercentEncoding.decode(bytes, 0, bytes.length);
        path = new String(bytes, 0, l);
        path = path.substring(1);
        if (path.startsWith(torrentInfo.name + "/"))
            path = path.substring((torrentInfo.name + "/").length());

        TorrentEntryPriority entryPriority = new TorrentEntryPriority(TIMESTAMP.now(), TIMESTAMP.now(), torrentInfo.id, path, priority);
        dbService.withBuilder(b -> {
            return entryPriority.id = dbService.insertInto(b, entryPriority, Tables.TORRENT_ENTRY_PRIORITY);
        });

        ActiveClient ac = clients.get(hash);
        if (ac != null) {
            ac.setPriority(path, priority);
        }
    }

    @Override
    public void pauseUpdater() {
        broadcasting = false;
    }

    @Override
    public void resumeUpdater() {
        broadcasting = true;
    }

    static class DataDescriptorModule implements Module {

        final EventBus<Events> eventBus;

        DataDescriptorModule(EventBus<Events> eventBus) {
            this.eventBus = eventBus;
        }

        @Override
        public void configure(Binder binder) {
        }

        @Provides
        @Singleton
        public IDataDescriptorFactory provideDataDescriptorFactory(Config config, EventSource eventSource, ChunkVerifier verifier) {
            DataReaderFactory dataReaderFactory = new DataReaderFactory(eventSource);
            return (torrent, storage) -> new ObservedDataDescriptor(storage, torrent, eventBus, verifier, dataReaderFactory, config.getTransferBlockSize());
        }

        @Provides
        @Singleton
        public ChunkVerifier provideVerifier(Config config, Digester digester) {
            return new ObservedChunkVerifier(digester, config.getNumOfHashingThreads());
        }

        @Provides
        @Singleton
        public EventBus<Events> provideEventBus() {
            return eventBus;
        }


        @Provides
        @Singleton
        public DataWorker provideDataWorker(
                IRuntimeLifecycleBinder lifecycleBinder,
                TorrentRegistry torrentRegistry,
                ChunkVerifier verifier,
                BlockCache blockCache,
                Config config) {
            return new CachedDataWorker(lifecycleBinder, torrentRegistry, verifier, blockCache, config);
        }
    }


    @Override
    public void init() {
        configureSecurity();

        //todo: remove this.listenerExecutor = Executors.newSingleThreadScheduledExecutor(); from DefaultClient implements BtClient
        //todo: rewrite MessageDispatcher without busy loop
        //todo: save to db number of downloaded chunks and show progress based on it - need to remove rtorrent first =(
        //todo: rewrite pool of threads for socket-creation to a single nio-thread
        //todo: sort files
        //todo: change torrent's status to SEEDING when download finished
        //todo: onResume - restore list of chunks without rechecking

        Config config = createDefaultConfig();

        btRuntime = new BtRuntimeBuilder(config)
                .autoLoadModules()
                .module(createDHTModule())
                .module(new HttpTrackerModule())
                .module(new DataDescriptorModule(eventBus))
                .disableAutomaticShutdown()
                .disablePeerExchange()
                .disableLocalServiceDiscovery()
                .build();

        downloadsDir = getDownloadPath().toFile();
        btRuntime.startup();
        torrentRegistry = (AdhocTorrentRegistry) btRuntime.getInjector().getInstance(TorrentRegistry.class);
        eventSink = btRuntime.getInjector().getInstance(EventSink.class);


        AtomicLong lastVerificationEvent = new AtomicLong();
        eventBus.on(Events.VERIFICATION_UPDATE, (event, data) -> {
            Events.VerificationUpdateData d = (Events.VerificationUpdateData) data;
            if (System.currentTimeMillis() - lastVerificationEvent.get() > 250 || d.processed == d.total) {
                lastVerificationEvent.set(System.currentTimeMillis());
                System.out.println(data);
                ActiveClient activeClient = clients.get(d.torrentHash);
                if (d.processed == d.total) {
                    if (d.verified == d.total) {
                        activeClient.torrentInfo.setStatus(TorrentInfo.Status.SEEDING);
                        activeClient.updatedEvent.progress = 100f;
                        storeTorrentBitfield(d.torrentHash);
                    } else {
                        activeClient.updatedEvent.progress = d.verified * 100f / d.total;
                        activeClient.torrentInfo.setStatus(TorrentInfo.Status.DOWNLOADING);
                    }
                } else {
                    activeClient.torrentInfo.setStatus(TorrentInfo.Status.CHECKING);
                    activeClient.updatedEvent.progress = d.processed * 100f / d.total;
                }

                activeClient.updatedEvent.status = activeClient.torrentInfo.getStatus().name();
                appWebSocketHandler.addBroadcastTask(activeClient.updatedEvent);
            }
        });

        resumeDownloading();
    }

    private void configureSecurity() {
        // Starting with JDK 8u152 this is a way to programmatically allow unlimited encryption
        // See http://www.oracle.com/technetwork/java/javase/8u152-relnotes-3850503.html
        String key = "crypto.policy";
        String value = "unlimited";
        try {
            Security.setProperty(key, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
