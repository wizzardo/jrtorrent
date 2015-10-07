package com.wizzardo.jrt;

import com.wizzardo.tools.collections.CollectionTools;
import com.wizzardo.tools.xml.Node;
import com.wizzardo.tools.xml.XmlParser;

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

    private String executeRequest(XmlRpc request) {
        return new ScgiClient.Request(host, port, request.render()).get();
    }

    public static void main(String[] args) throws InterruptedException {

        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 5000;
        for (int i = 0; i < 10; i++) {

//        ScgiClient.XmlRpc.Params params = new ScgiClient.XmlRpc.Params();
//        List<ScgiClient.XmlRpc.Params> list = new ArrayList<>();
            long time = System.currentTimeMillis();
            List<TorrentInfo> torrents = new RTorrentClient(host, port).getTorrents();
            time = System.currentTimeMillis() - time;
            for (TorrentInfo info : torrents) {
                System.out.println(info);
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
