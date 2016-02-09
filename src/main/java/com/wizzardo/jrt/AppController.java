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
        String link = request.entry("url").asString();
        boolean autostart = "on".equals(request.entry("autostart").asString());
        byte[] file = request.entry("file").asBytes();

        if (file.length != 0) {
            File tempFile = File.createTempFile("jrt", "torrent");
            tempFile.deleteOnExit();
            FileTools.bytes(tempFile, file);
            rtorrentService.load(tempFile.getAbsolutePath(), autostart);
        } else if (!link.isEmpty()) {
            rtorrentService.load(link, autostart);
        } else {
            throw new IllegalArgumentException("link and file are empty");
        }
        return renderString("ok");
    }
}
