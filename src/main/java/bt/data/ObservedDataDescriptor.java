package bt.data;

import bt.BtException;
import bt.data.*;
import bt.data.range.*;
import bt.metainfo.Torrent;
import bt.metainfo.TorrentFile;
import bt.torrent.TorrentDescriptor;
import com.wizzardo.jrt.bt.Events;
import com.wizzardo.jrt.bt.ObservedChunkVerifier;
import com.wizzardo.tools.misc.Stopwatch;
import com.wizzardo.tools.misc.Unchecked;
import com.wizzardo.tools.misc.event.EventBus;
import com.wizzardo.tools.misc.event.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ObservedDataDescriptor implements DataDescriptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDataDescriptor.class);

    private Storage storage;

    private EventBus<Events> eventBus;
    private Torrent torrent;
    private List<ChunkDescriptor> chunkDescriptors;
    private Bitfield bitfield;

    private Map<Integer, List<TorrentFile>> filesForPieces;
    private Set<StorageUnit> storageUnits;
    private DataReader reader;

    private ChunkVerifier verifier;

    public ObservedDataDescriptor(Storage storage,
                                  Torrent torrent,
                                  EventBus<Events> eventBus,
                                  ChunkVerifier verifier,
                                  DataReaderFactory dataReaderFactory,
                                  int transferBlockSize) {
        this.storage = storage;
        this.torrent = torrent;
        this.eventBus = eventBus;
        this.verifier = verifier;

        init(transferBlockSize);

        this.reader = dataReaderFactory.createReader(torrent, this);
    }

    private void init(long transferBlockSize) {
        List<TorrentFile> files = torrent.getFiles();

        long totalSize = torrent.getSize();
        long chunkSize = torrent.getChunkSize();

        if (transferBlockSize > chunkSize) {
            transferBlockSize = chunkSize;
        }

        int chunksTotal = (int) Math.ceil(totalSize / chunkSize);
        Map<Integer, List<TorrentFile>> filesForPieces = new HashMap<>((int) (chunksTotal / 0.75d) + 1);
        List<ChunkDescriptor> chunks = new ArrayList<>(chunksTotal + 1);

        Iterator<byte[]> chunkHashes = torrent.getChunkHashes().iterator();

        Map<StorageUnit, TorrentFile> storageUnitsToFilesMap = new LinkedHashMap<>((int) (files.size() / 0.75d) + 1);
        files.forEach(f -> storageUnitsToFilesMap.put(storage.getUnit(torrent, f), f));

        // filter out empty files (and create them at once)
        List<StorageUnit> nonEmptyStorageUnits = new ArrayList<>();
        for (StorageUnit unit : storageUnitsToFilesMap.keySet()) {
            if (unit.capacity() > 0) {
                nonEmptyStorageUnits.add(unit);
            } else {
                try {
                    // TODO: think about adding some explicit "initialization/creation" method
                    if (unit.writeBlock(new byte[0], 0) < 0) {
                        throw new IllegalStateException("Failed to initialize storage unit: " + unit);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to create empty storage unit: " + unit, e);
                }
            }
        }

        if (nonEmptyStorageUnits.size() > 0) {
            long limitInLastUnit = nonEmptyStorageUnits.get(nonEmptyStorageUnits.size() - 1).capacity();
            DataRange data = new ReadWriteDataRange(nonEmptyStorageUnits, 0, limitInLastUnit);

            long off, lim;
            long remaining = totalSize;
            while (remaining > 0) {
                off = chunks.size() * chunkSize;
                lim = Math.min(chunkSize, remaining);

                DataRange subrange = data.getSubrange(off, lim);

                if (!chunkHashes.hasNext()) {
                    throw new BtException("Wrong number of chunk hashes in the torrent: too few");
                }

                List<TorrentFile> chunkFiles = new ArrayList<>();
                subrange.visitUnits((unit, off1, lim1) -> chunkFiles.add(storageUnitsToFilesMap.get(unit)));
                filesForPieces.put(chunks.size(), chunkFiles);

                chunks.add(buildChunkDescriptor(subrange, transferBlockSize, chunkHashes.next()));

                remaining -= chunkSize;
            }
        }

        if (chunkHashes.hasNext()) {
            throw new BtException("Wrong number of chunk hashes in the torrent: too many");
        }

        this.bitfield = buildBitfield(chunks);
        this.chunkDescriptors = chunks;
        this.storageUnits = storageUnitsToFilesMap.keySet();
        this.filesForPieces = filesForPieces;
    }

    private ChunkDescriptor buildChunkDescriptor(DataRange data, long blockSize, byte[] checksum) {
        BlockRange<DataRange> blockData = Ranges.blockRange(data, blockSize);
        SynchronizedRange<BlockRange<DataRange>> synchronizedRange = new SynchronizedRange<>(blockData);
        SynchronizedDataRange<BlockRange<DataRange>> synchronizedData =
                new SynchronizedDataRange<>(synchronizedRange, BlockRange::getDelegate);
        SynchronizedBlockSet synchronizedBlockSet = new SynchronizedBlockSet(blockData.getBlockSet(), synchronizedRange);

        return new DefaultChunkDescriptor(synchronizedData, synchronizedBlockSet, checksum);
    }

    private Bitfield buildBitfield(List<ChunkDescriptor> chunks) {
        System.out.println("ObservedDataDescriptor.buildBitfield start "+eventBus);
        Stopwatch stopwatch = new Stopwatch("verification");
        Bitfield bitfield = new Bitfield(chunks.size());

        AtomicBoolean running = new AtomicBoolean(true);
        String hash = torrent.getTorrentId().toString();
        Listener<Events, ?> listener = (event, data) -> {
            System.out.println("on STOP_TORRENT "+hash.equals(data));
            if (hash.equals(data))
                running.set(false);
        };
        eventBus.on(Events.STOP_TORRENT, listener);

        if (verifier instanceof ObservedChunkVerifier) {
            try {
                ((ObservedChunkVerifier) verifier).verify(chunks, bitfield, (processed, verified, total) -> {
//                System.out.println(processed + "/" + total + " verified: " + verified);
                    eventBus.trigger(Events.VERIFICATION_UPDATE, new Events.VerificationUpdateData(hash, processed, verified, total));
                    return running.get();
                });
            }catch (Exception e){
                e.printStackTrace();
                throw Unchecked.rethrow(e);
            }
        } else {
            verifier.verify(chunks, bitfield);
        }
        eventBus.off(Events.STOP_TORRENT, listener);
        System.out.println(stopwatch);
        System.out.println("ObservedDataDescriptor.buildBitfield complete");
        return bitfield;
    }

    @Override
    public List<ChunkDescriptor> getChunkDescriptors() {
        return chunkDescriptors;
    }

    @Override
    public Bitfield getBitfield() {
        return bitfield;
    }

    @Override
    public List<TorrentFile> getFilesForPiece(int pieceIndex) {
        if (pieceIndex < 0 || pieceIndex >= bitfield.getPiecesTotal()) {
            throw new IllegalArgumentException("Invalid piece index: " + pieceIndex +
                    ", expected 0.." + bitfield.getPiecesTotal());
        }
        return filesForPieces.get(pieceIndex);
    }

    @Override
    public DataReader getReader() {
        return reader;
    }

    @Override
    public void close() {
        storageUnits.forEach(unit -> {
            try {
                unit.close();
            } catch (Exception e) {
                LOGGER.error("Failed to close storage unit: " + unit);
            }
        });
    }

    @Override
    public String toString() {
        return this.getClass().getName() + " <" + torrent.getName() + ">";
    }
}
