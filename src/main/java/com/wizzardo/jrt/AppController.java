package com.wizzardo.jrt;

import com.wizzardo.http.filter.TokenFilter;
import com.wizzardo.http.framework.Controller;
import com.wizzardo.http.framework.ControllerUrlMapping;
import com.wizzardo.http.framework.Holders;
import com.wizzardo.http.framework.di.DependencyFactory;
import com.wizzardo.http.framework.template.Renderer;
import com.wizzardo.tools.collections.CollectionTools;
import com.wizzardo.tools.io.FileTools;
import com.wizzardo.tools.json.JsonTools;

import java.io.File;
import java.io.IOException;
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
    TokenFilter tokenFilter = getTokenFilter();

    private TokenFilter getTokenFilter() {
        try {
            return DependencyFactory.get(TokenFilter.class);
        } catch (IllegalStateException ignored) {
        }
        return null;
    }

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
                        .add("token", tokenFilter != null ? tokenFilter.generateToken(request) : "")
                        .add("downloadsPath", Holders.getApplication().getUrlMapping().getUrlTemplate("downloads").getRelativeUrl())
                        .get()
        ));

        return renderView("index");
    }

    public Renderer addTorrent() throws IOException {
        if (request.isMultipart()) {
            request.prepareMultiPart();

            String link = request.entry("url").asString();
            byte[] file = request.entry("file").asBytes();

//            System.out.println("link: " + link);
//            System.out.println("file: " + file.length + " " + MD5.create().update(file).asString());

            if (file.length != 0) {
                File tempFile = File.createTempFile("jrt", "torrent");
                tempFile.deleteOnExit();
                FileTools.bytes(tempFile, file);
                rtorrentService.load(tempFile.getAbsolutePath());
            } else if (!link.isEmpty()) {
                rtorrentService.load(link);
            } else {
                throw new IllegalArgumentException("link and file are empty");
            }
        }

        return renderString("ok");
    }
}
