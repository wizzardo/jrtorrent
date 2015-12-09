package com.wizzardo.jrt;

import com.wizzardo.http.framework.Controller;
import com.wizzardo.http.framework.template.Renderer;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by wizzardo on 07.12.15.
 */
public class AppController extends Controller {

    RTorrentService rtorrentService;

    public Renderer index() {
        List<TorrentInfo> torrents = rtorrentService.list();
        model().append("torrents", torrents);

        Map<String, Collection<TorrentEntry>> entries = new HashMap<>();
        model().append("entries", entries);
        for (TorrentInfo torrent : torrents) {
            entries.put(torrent.getHash(), rtorrentService.entries(torrent));
        }
        return renderView("index__");
    }

    public Renderer riotIndex() {
        return renderView("index");
    }
}
