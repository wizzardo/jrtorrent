package com.wizzardo.jrt;

import com.wizzardo.http.filter.TokenFilter;
import com.wizzardo.http.framework.Controller;
import com.wizzardo.http.framework.ControllerUrlMapping;
import com.wizzardo.http.framework.Holders;
import com.wizzardo.http.framework.di.DependencyFactory;
import com.wizzardo.http.framework.template.Renderer;
import com.wizzardo.http.mapping.Path;
import com.wizzardo.http.request.MultiPartEntry;
import com.wizzardo.tools.io.FileTools;
import com.wizzardo.tools.json.JsonTools;
import com.wizzardo.tools.misc.With;

import java.io.File;
import java.io.IOException;

import static com.wizzardo.tools.misc.With.with;

/**
 * Created by wizzardo on 07.12.15.
 */
public class AppController extends Controller {

    TorrentClientService torrentClientService;
    ControllerUrlMapping mapping = DependencyFactory.get(ControllerUrlMapping.class);
    TokenFilter tokenFilter = getTokenFilter();
    TagBundler tagBundler;

    private TokenFilter getTokenFilter() {
        try {
            return DependencyFactory.get(TokenFilter.class);
        } catch (IllegalStateException ignored) {
        }
        return null;
    }

    static class AppConfig {
        String ws;
        String addTorrent;
        String token;
        String downloadsPath;
        String zipPath;
        String m3uPath;
    }

    public Renderer index() {
        model().append("config", JsonTools.serialize(with(new AppConfig(), it -> {
            it.ws = mapping.getUrlTemplate("ws").getRelativeUrl();
            it.addTorrent = mapping.getUrlTemplate(AppController.class, "addTorrent").getRelativeUrl();
            it.token = tokenFilter != null ? tokenFilter.generateToken(request) : "";
            ControllerUrlMapping mapping = Holders.getApplication().getUrlMapping();
            it.downloadsPath = mapping.getUrlTemplate("downloads").getRelativeUrl();
            it.zipPath = mapping.getUrlTemplate("zip").getRelativeUrl();
            it.m3uPath = mapping.getUrlTemplate("m3u").getRelativeUrl();
        })));

        return renderView("index");
    }

    static class LoginData {
        String token;
    }

    public LoginData self() {
        LoginData loginData = new LoginData();
        loginData.token = tokenFilter != null ? tokenFilter.generateToken(request) : "";
        return loginData;
    }

    public Renderer tags() {
        StringBuilder sb = new StringBuilder();
        sb.append(tagBundler.toJavascript("add_button"));
        sb.append(tagBundler.toJavascript("delete_form"));
        sb.append(tagBundler.toJavascript("mdl_select"));
        sb.append(tagBundler.toJavascript("modal"));
        sb.append(tagBundler.toJavascript("torrent"));
        sb.append(tagBundler.toJavascript("torrents"));
        sb.append(tagBundler.toJavascript("tree"));
        sb.append(tagBundler.toJavascript("tree_entry"));
        sb.append(tagBundler.toJavascript("upload_form"));
        sb.append(tagBundler.toJavascript("disk_status"));
        return renderString(sb.toString());
    }

    public Renderer addTorrent() throws IOException {
        String link = request.entry("url").asString();
        boolean autostart = With.map(request.entry("autostart"), it -> it != null && "on".equals(it.asString()));
        MultiPartEntry entry = request.entry("file");
        byte[] file = entry == null ? new byte[0] : entry.asBytes();

        if (file.length != 0) {
            File tempFile = File.createTempFile("jrt", "torrent");
            tempFile.deleteOnExit();
            FileTools.bytes(tempFile, file);
            torrentClientService.load(tempFile.getAbsolutePath(), autostart);
        } else if (!link.isEmpty()) {
            torrentClientService.load(link, autostart);
        } else {
            throw new IllegalArgumentException("link and file are empty");
        }
        return renderJson("{}");
    }
}
