package com.wizzardo.jrt;

import com.wizzardo.http.framework.Holders;
import com.wizzardo.http.framework.di.Service;
import com.wizzardo.tools.evaluation.Config;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by wizzardo on 07.12.15.
 */
public class RTorrentService implements Service {

    private RTorrentClient client;

    public RTorrentService() {
        Config rtorrentConfig = Holders.getConfig().config("jrt").config("rtorrent");
        String host = rtorrentConfig.get("host", "localhost");
        int port = rtorrentConfig.get("port", 5000);
        client = new RTorrentClient(host, port);
    }

    public List<TorrentInfo> list() {
        return client.getTorrents();
    }

    public Collection<TorrentEntry> entries(TorrentInfo ti) {
        return client.getEntries(ti);
    }

    public Collection<TorrentEntry> entries(String hash) {
        return client.getEntries(hash);
    }

    public void load(String torrent) {
        client.load(torrent);
    }

    public void start(String torrent) {
        client.start(torrent);
    }

    public void stop(String torrent) {
        client.stop(torrent);
    }
}
