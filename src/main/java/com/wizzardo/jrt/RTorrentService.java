package com.wizzardo.jrt;

import com.wizzardo.http.framework.di.Service;

import java.util.Collection;
import java.util.List;

/**
 * Created by wizzardo on 07.12.15.
 */
public class RTorrentService implements Service {

    private RTorrentClient client;

    public RTorrentService() {
        client = new RTorrentClient("localhost", 5000);
    }

    public List<TorrentInfo> list() {
        return client.getTorrents();
    }

    public Collection<TorrentEntry> entries(TorrentInfo ti) {
        return client.getEntries(ti);
    }
}
