package com.wizzardo.jrt;

import com.wizzardo.tools.collections.CollectionTools;
import com.wizzardo.tools.xml.Node;
import com.wizzardo.tools.xml.XmlParser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by wizzardo on 03.10.15.
 */
public class RTorrentClient {

    private String host;
    private int port;

    public RTorrentClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public int getFilesCount(TorrentInfo torrent) {
        String response = new XmlParser().parse(executeRequest(new XmlRpc("d.get_size_files", new XmlRpc.Params().add(torrent.getHash()))))
//                .get("params/param/value/i8").text();
                .get(0).get(0).get(0).get(0).text();

        return Integer.parseInt(response);
    }

    public String getFile(TorrentInfo torrent, int i) {
        String file = new XmlParser().parse(executeRequest(new XmlRpc("f.get_path", new XmlRpc.Params().add(torrent.getHash()).add(i))))
//                .get("params/param/value/string").text();
                .get(0).get(0).get(0).get(0).text();

        return file;
    }

    public void load(String torrent) {
        executeRequest(new XmlRpc("load", new XmlRpc.Params().add(torrent)));
    }

    public void stop(TorrentInfo torrent) {
        executeRequest(new XmlRpc("d.stop", new XmlRpc.Params().add(torrent.getHash())));
    }

    public void start(TorrentInfo torrent) {
        executeRequest(new XmlRpc("d.start", new XmlRpc.Params().add(torrent.getHash())));
    }

    public void pause(TorrentInfo torrent) {
        executeRequest(new XmlRpc("d.pause", new XmlRpc.Params().add(torrent.getHash())));
    }

    public void resume(TorrentInfo torrent) {
        executeRequest(new XmlRpc("d.resume", new XmlRpc.Params().add(torrent.getHash())));
    }

    private Collection<TorrentEntry> getEntries(TorrentInfo torrent) {
        return getEntries(torrent.getHash());
    }

    private Collection<TorrentEntry> getEntries(String hash) {
        XmlRpc.Params params = new XmlRpc.Params();
        params.add(hash)
                .add(0)
                .add("f.get_path_components=")
                .add("f.get_priority=")
                .add("f.get_completed_chunks=")
                .add("f.get_size_chunks=")
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
                entry = entry.getOrCreate(name);
            }
            entry.setId(id++);
            entry.setPriority(FilePriority.byString(data.get(1).get(0).get(0).text()));
            entry.setChunksCompleted(Integer.parseInt(data.get(2).get(0).get(0).text()));
            entry.setChunksCount(Integer.parseInt(data.get(3).get(0).get(0).text()));
        }

//        System.out.println(files.toXML(true));
        return root.getChildren().values();
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
            info.setName(data.get(0).get(0).text());
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
            return null;
        });
        return torrents;
    }

    public void setFilePriority(TorrentInfo torrent, TorrentEntry entry, FilePriority priority) {
        setFilePriority(torrent.getHash(), entry.getId(), priority);
    }

    public void setFilePriority(String hash, int file, FilePriority priority) {
        XmlRpc.Params params = new XmlRpc.Params();
        params.add(hash)
                .add(file)
                .add(priority.i)
        ;
        String s = new RTorrentClient(host, port).executeRequest(new XmlRpc("f.set_priority", params));
//        System.out.println(s);
        s = new RTorrentClient(host, port).executeRequest(new XmlRpc("d.update_priorities", hash));
//        System.out.println(s);
    }

    public String getDownloadDirectory(){
        return new XmlParser().parse(executeRequest(new XmlRpc("get_directory"))).get(0).text();
    }

    private String executeRequest(XmlRpc request) {
        return new ScgiClient.Request(host, port, request.render()).get();
    }

    public static void main(String[] args) throws InterruptedException {

        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 5000;
        for (int i = 0; i < 1; i++) {

//        ScgiClient.XmlRpc.Params params = new ScgiClient.XmlRpc.Params();
//        List<ScgiClient.XmlRpc.Params> list = new ArrayList<>();
            long time = System.currentTimeMillis();
            List<TorrentInfo> torrents = new RTorrentClient(host, port).getTorrents();
            time = System.currentTimeMillis() - time;
            for (TorrentInfo info : torrents) {
                System.out.println(info);
//                new RTorrentClient(host, port).getEntries(info);
//            params.add(new ScgiClient.XmlRpc("t.multicall", info.getHash(), "d.get_hash=", "t.get_scrape_incomplete="));
            }

            System.out.println("get torrents info: " + time + " ms");
            System.out.println("");

            Thread.sleep(1000);
        }
//        String request = new ScgiClient.XmlRpc(host, params).render();
//        System.out.println(new ScgiClient.Request(host, port, request).get());


//        if (args.length > 2) {
//            String[] moreArgs = args.length > 3 ? Arrays.copyOfRange(args, 3, args.length) : new String[0];
//            String request = new ScgiClient.XmlRpc(args[2], moreArgs).render();
//            System.out.println(new ScgiClient.Request(host, port, request).get());
//        }


    }
}
