package com.wizzardo.jrt;

import com.wizzardo.tools.cache.Cache;
import com.wizzardo.tools.collections.CollectionTools;
import com.wizzardo.tools.collections.flow.Flow;
import com.wizzardo.tools.interfaces.Consumer;
import com.wizzardo.tools.io.FileTools;
import com.wizzardo.tools.xml.Node;
import com.wizzardo.tools.xml.XmlParser;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by wizzardo on 03.10.15.
 */
public class RTorrentClient {

    private String host;
    private int port;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Cache<Long, Runnable> delayed = new Cache<Long, Runnable>(0) {
        @Override
        public void onRemoveItem(Long aLong, Runnable runnable) {
            executor.submit(runnable);
        }
    };

    public RTorrentClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public int getFilesCount(TorrentInfo torrent) {
        return getFilesCount(torrent.getHash());
    }

    public int getFilesCount(String hash) {
        String response = new XmlParser().parse(executeRequest(new XmlRpc("d.get_size_files", new XmlRpc.Params().add(hash))))
//                .get("params/param/value/i8").text();
                .get(0).get(0).get(0).get(0).text();

        return Integer.parseInt(response);
    }

    public String getFile(TorrentInfo torrent, int i) {
        return getFile(torrent.getHash(), i);
    }

    public String getFile(String hash, int i) {
        String file = new XmlParser().parse(executeRequest(new XmlRpc("f.get_path", new XmlRpc.Params().add(hash).add(i))))
//                .get("params/param/value/string").text();
                .get(0).get(0).get(0).get(0).text();

        return file;
    }

    public long getFileSize(String hash, int i) {
        String size = new XmlParser().parse(executeRequest(new XmlRpc("f.get_size_bytes", new XmlRpc.Params().add(hash).add(i))))
//                .get("params/param/value/i8").text();
                .get(0).get(0).get(0).get(0).text();

        return Long.parseLong(size);
    }

    public long getFileSizeChunks(String hash, int i) {
        String size = new XmlParser().parse(executeRequest(new XmlRpc("f.get_size_chunks", new XmlRpc.Params().add(hash).add(i))))
//                .get("params/param/value/i8").text();
                .get(0).get(0).get(0).get(0).text();

        return Long.parseLong(size);
    }

    public long getFileCompletedChunks(String hash, int i) {
        String size = new XmlParser().parse(executeRequest(new XmlRpc("f.get_completed_chunks", new XmlRpc.Params().add(hash).add(i))))
//                .get("params/param/value/i8").text();
                .get(0).get(0).get(0).get(0).text();

        return Long.parseLong(size);
    }

    public void delayed(long ms, Consumer<RTorrentClient> consumer) {
        delayed.put(System.nanoTime(), () -> consumer.consume(this), ms);
    }

    public void load(String torrent) {
        load(torrent, true);
    }

    public void load(String torrent, boolean autostart) {
        List<TorrentInfo> before = getTorrents();
        executeRequest(new XmlRpc("load", torrent));
        delayed(2000, client -> {
            List<TorrentInfo> after = getTorrents();
            for (TorrentInfo info : before) {
                after.removeIf(it -> it.getHash().equals(info.getHash()));
            }
            TorrentInfo ti = after.get(0);
            System.out.println("start: " + ti.getHash());
            if (autostart || torrent.startsWith("magnet"))
                client.start(ti);

            if (torrent.startsWith("magnet")) {
                if (autostart)
                    delayed(5000, c -> {
                        System.out.println("start: " + ti.getHash());
                        c.start(ti);
                    });
                delayed(20000, cc -> {
                    System.out.println("remove file: " + ti.getHash() + ".meta");
                    new File(cc.getDownloadDirectory(), ti.getHash() + ".meta").delete();
                });
            }
        });
    }

    public void stop(TorrentInfo torrent) {
        stop(torrent.getHash());
    }

    public void stop(String hash) {
        executeRequest(new XmlRpc("d.close", hash));
    }

    public void start(TorrentInfo torrent) {
        start(torrent.getHash());
    }

    public void start(String hash) {
        executeRequest(new XmlRpc("d.start", hash));
    }

    public void pause(TorrentInfo torrent) {
        pause(torrent.getHash());
    }

    public void pause(String hash) {
        executeRequest(new XmlRpc("d.pause", hash));
    }

    public void resume(TorrentInfo torrent) {
        resume(torrent.getHash());
    }

    public void resume(String hash) {
        executeRequest(new XmlRpc("d.resume", hash));
    }

    public void remove(TorrentInfo torrent) {
        remove(torrent.getHash());
    }

    public void remove(String hash) {
        executeRequest(new XmlRpc("d.erase", hash));
    }

    public void removeWithData(TorrentInfo torrent) {
        removeWithData(torrent.getHash());
    }

    public void removeWithData(String hash) {
        File path;
        if (getFilesCount(hash) == 1)
            path = new File(getDownloadDirectory(), getFile(hash, 0));
        else
            path = new File(getTorrentDirectory(hash));

        executeRequest(new XmlRpc("d.erase", hash));
        FileTools.deleteRecursive(path);
    }

    public Collection<TorrentEntry> getEntries(TorrentInfo torrent) {
        return getEntries(torrent.getHash());
    }

    public TorrentEntry getRootEntry(String hash) {
        XmlRpc.Params params = new XmlRpc.Params();
        params.add(hash)
                .add(0)
                .add("f.get_path_components=")
                .add("f.get_priority=")
                .add("f.get_completed_chunks=")
                .add("f.get_size_chunks=")
                .add("f.get_size_bytes=")
        ;
//        for (int i = 0; i < l; i++) {
//            params.add("f.get_path").add("D4264E9D08C1F6BD9BCFC1D4B47E149C577D1FFC").add(i);
//        }
        String s = executeRequest(new XmlRpc("f.multicall", params));
        Node xml = new XmlParser().parse(s);
        Node files = xml.get(0).get(0).get(0).get(0).get(0);

        TorrentEntry root = new TorrentEntry();
        int id = 0;
        for (Node child : files.children()) {
            Node data = child.get(0).get(0);
            Node path = data.get(0).get(0).get(0);

            TorrentEntry entry = root;
            for (int i = 0; i < path.size(); i++) {
                String name = path.get(i).get(0).get(0).text();
                name = Node.unescape(name);
                entry = entry.getOrCreate(name);
            }
            entry.setId(id++);
            entry.setPriority(FilePriority.byString(data.get(1).get(0).get(0).text()));
            entry.setChunksCompleted(Integer.parseInt(data.get(2).get(0).get(0).text()));
            entry.setChunksCount(Integer.parseInt(data.get(3).get(0).get(0).text()));
            entry.setSizeBytes(Long.parseLong(data.get(4).get(0).get(0).text()));
        }

//        System.out.println(files.toXML(true));
        return root;
    }

    public Collection<TorrentEntry> getEntries(String hash) {
        TorrentEntry rootEntry = getRootEntry(hash);
        if (rootEntry.getChildren().size() != 1 || Flow.of(rootEntry.getChildren()).all(it -> it.getValue().isFolder()).get()) {
            return Collections.singleton(new TorrentEntry(getName(hash), rootEntry.getChildren()));
        } else {
            return rootEntry.getChildren().values();
        }
    }

    public String getName(String hash) {
        String name = new XmlParser().parse(executeRequest(new XmlRpc("d.name", hash))).get(0).text();
        return Node.unescape(name);
    }

    public List<TorrentInfo> getTorrents() {
        String response = executeRequest(new XmlRpc("d.multicall", "main",
                "d.name=",
                "d.hash=",
                "d.size_bytes=",
                "d.bytes_done=",
                "d.get_down_rate=",
                "d.get_up_rate=",
                "d.get_complete=",
                "d.is_open=",
                "d.is_hash_checking=",
                "d.get_state=",
                "d.get_peers_complete=",
                "d.get_peers_accounted=",
                "d.get_up_total="
        ));
        XmlParser parser = new XmlParser();
        Node methodResponse = parser.parse(response);
//        System.out.println("response: " + methodResponse.toXML(true));
//        List<TorrentInfo> torrents = CollectionTools.collect(methodResponse.getAll("params/param/value/array/data/value/array/data"), data -> {
        List<TorrentInfo> torrents = CollectionTools.collect(methodResponse.get(0).get(0).get(0).get(0).get(0).children(), data -> {
            data = data.get(0).get(0);
            TorrentInfo info = new TorrentInfo();
            info.setName(Node.unescape(data.get(0).get(0).text()));
            info.setHash(data.get(1).get(0).text());
            info.setSize(Long.parseLong(data.get(2).get(0).text()));
            info.setDownloaded(Long.parseLong(data.get(3).get(0).text()));
            info.setDownloadSpeed(Long.parseLong(data.get(4).get(0).text()));
            info.setUploadSpeed(Long.parseLong(data.get(5).get(0).text()));
            info.setStatus(TorrentInfo.Status.valueOf("1".equals(data.get(6).get(0).text()), "1".equals(data.get(7).get(0).text()), "1".equals(data.get(8).get(0).text()), "1".equals(data.get(9).get(0).text())));
            info.setSeeds(Long.parseLong(data.get(10).get(0).text()));
            info.setPeers(Long.parseLong(data.get(11).get(0).text()));
            info.setUploaded(Long.parseLong(data.get(12).get(0).text()));
            return info;
        });


        XmlRpc.Params params = new XmlRpc.Params();
        for (TorrentInfo info : torrents) {
            params.add(new XmlRpc("t.multicall", info.getHash(), "d.get_hash=", "t.get_scrape_incomplete=", "t.get_scrape_complete="));
        }

        response = executeRequest(new XmlRpc("system.multicall", new XmlRpc.Params().add(params)));
        Node multi = parser.parse(response);
        CollectionTools.eachWithIndex(multi.get(0).get(0).get(0).get(0).get(0).children(), (i, node) -> {
            int seeds = 0;
            int peers = 0;
            for (Node values : node.get(0).get(0).get(0).get(0).get(0).children()) {
                peers += Integer.parseInt(values.get(0).get(0).get(0).text());
                seeds += Integer.parseInt(values.get(0).get(0).get(1).text());
            }
            torrents.get(i).setTotalSeeds(seeds);
            torrents.get(i).setTotalPeers(peers);
        });
        return torrents;
    }

    public void setFilePriority(TorrentInfo torrent, TorrentEntry entry, FilePriority priority) {
        setFilePriority(torrent.getHash(), entry.getId(), priority);
    }

    public void setFilePriority(String hash, int file, FilePriority priority) {
        setFilePriority(hash, file, priority, true);
    }

    public void setFilePriority(String hash, int file, FilePriority priority, boolean updatePriorities) {
        XmlRpc.Params params = new XmlRpc.Params();
        params.add(hash)
                .add(file)
                .add(priority.i)
        ;
        String s = executeRequest(new XmlRpc("f.set_priority", params));
        if (updatePriorities)
            updateFilePriorities(hash);
    }

    public TorrentEntry findEntry(String path, TorrentEntry root) {
        String[] parts = path.split("/");
        if ((parts.length == 1 && parts[0].isEmpty()) || parts.length == 0)
            return root;

        Collection<TorrentEntry> entries = root.getChildren().values();
        outer:
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty())
                continue;

            for (TorrentEntry entry : entries) {
                if (entry.name.equals(part)) {
                    if (i == parts.length - 1)
                        return entry;

                    if (!entry.isFolder())
                        return null;

                    entries = entry.getChildren().values();
                    continue outer;
                }
            }
        }
        return null;
    }

    public void setPriority(String hash, String path, FilePriority priority) {
        setPriority(hash, findEntry(path, getRootEntry(hash)), priority, true);
    }

    public void updateFilePriorities(String hash) {
        executeRequest(new XmlRpc("d.update_priorities", hash));
    }

    public void setPriority(String hash, TorrentEntry entry, FilePriority priority, boolean updatePriorities) {
        if (!entry.isFolder()) {
            setFilePriority(hash, entry.getId(), priority, false);
        } else {
            for (TorrentEntry te : entry.getChildren().values()) {
                setPriority(hash, te, priority, false);
            }
        }
        if (updatePriorities)
            updateFilePriorities(hash);
    }

    public String getDownloadDirectory() {
        return new XmlParser().parse(executeRequest(new XmlRpc("get_directory"))).get(0).text();
    }

    public String getTorrentDirectory(TorrentInfo info) {
        return getTorrentDirectory(info.getHash());
    }

    public String getTorrentDirectory(String hash) {
        return new XmlParser().parse(executeRequest(new XmlRpc("d.get_directory", hash))).get(0).text();
    }

    private String executeRequest(XmlRpc request) {
        String render = request.render();
//        System.out.println("request: " + render);
        String response = new ScgiClient.Request(host, port, render).get();
//        System.out.println("response: " + response);
        return response;
    }

    public static void main(String[] args) throws InterruptedException {

        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 5000;

        RTorrentClient client = new RTorrentClient(host, port);
        for (TorrentInfo info : client.getTorrents()) {
            System.out.println(info);
            if (info.getStatus() == TorrentInfo.Status.STOPPED)
                client.start(info);
            for (TorrentEntry entry : client.getEntries(info)) {
                System.out.println((entry.isFolder() ? "D: " : "F: ") + entry.name);
            }
            System.out.println("getFilesCount: " + client.getFilesCount(info));
            System.out.println("getFile: " + client.getFile(info, 0));
            System.out.println("getTorrentDirectory: " + client.getTorrentDirectory(info));
            System.out.println("getDownloadDirectory: " + client.getDownloadDirectory());
        }

//        client.load("/tmp/test.torrent");
//        client.remove("3E2C74D5C2A6DC62B1F6B8E655A56AB319FED56D");


//        for (int i = 0; i < 1; i++) {
//
////        ScgiClient.XmlRpc.Params params = new ScgiClient.XmlRpc.Params();
////        List<ScgiClient.XmlRpc.Params> list = new ArrayList<>();
//            long time = System.currentTimeMillis();
//            List<TorrentInfo> torrents = new RTorrentClient(host, port).getTorrents();
//            time = System.currentTimeMillis() - time;
//            for (TorrentInfo info : torrents) {
//                System.out.println(info);
////                new RTorrentClient(host, port).getEntries(info);
////            params.add(new ScgiClient.XmlRpc("t.multicall", info.getHash(), "d.get_hash=", "t.get_scrape_incomplete="));
//            }
//
//            System.out.println("get torrents info: " + time + " ms");
//            System.out.println("");
//
//            Thread.sleep(1000);
//        }
//        String request = new ScgiClient.XmlRpc(host, params).render();
//        System.out.println(new ScgiClient.Request(host, port, request).get());


//        if (args.length > 2) {
//            String[] moreArgs = args.length > 3 ? Arrays.copyOfRange(args, 3, args.length) : new String[0];
//            String request = new ScgiClient.XmlRpc(args[2], moreArgs).render();
//            System.out.println(new ScgiClient.Request(host, port, request).get());
//        }


    }
}
