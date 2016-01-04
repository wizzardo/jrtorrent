package com.wizzardo.jrt;

import com.wizzardo.http.framework.Controller;
import com.wizzardo.http.framework.ControllerUrlMapping;
import com.wizzardo.http.framework.di.DependencyFactory;
import com.wizzardo.http.framework.template.Renderer;
import com.wizzardo.tools.collections.CollectionTools;
import com.wizzardo.tools.json.JsonTools;
import com.wizzardo.tools.security.MD5;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by wizzardo on 07.12.15.
 */
public class AppController extends Controller {

    RTorrentService rtorrentService;
    ControllerUrlMapping mapping = DependencyFactory.get(ControllerUrlMapping.class);

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
        model().append("config", JsonTools.serialize(new CollectionTools.MapBuilder<>()
                        .add("ws", mapping.getUrlTemplate("ws").getRelativeUrl())
                        .add("addTorrent", mapping.getUrlTemplate(AppController.class, "addTorrent").getRelativeUrl())
                        .get()
        ));

        return renderView("index");
    }

    public Renderer addTorrent() {
        if (request.isMultipart()) {
            request.prepareMultiPart();

            String link = request.entry("url").asString();
            byte[] file = request.entry("file").asBytes();

            System.out.println("link: " + link);
            System.out.println("file: " + file.length + " " + MD5.create().update(file).asString());
        }

        return renderString("ok");
    }
}
