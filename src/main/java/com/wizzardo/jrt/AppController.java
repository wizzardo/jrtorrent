package com.wizzardo.jrt;

import com.wizzardo.http.filter.TokenFilter;
import com.wizzardo.http.framework.Controller;
import com.wizzardo.http.framework.di.DependencyFactory;
import com.wizzardo.http.framework.template.Renderer;
import com.wizzardo.http.request.MultiPartEntry;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Status;
import com.wizzardo.tools.io.FileTools;
import com.wizzardo.tools.misc.With;

import java.io.File;
import java.io.IOException;

/**
 * Created by wizzardo on 07.12.15.
 */
public class AppController extends Controller {

    AppConfig appConfig;
    TorrentClientService torrentClientService;
    TokenFilter tokenFilter = getTokenFilter();

    private TokenFilter getTokenFilter() {
        try {
            return DependencyFactory.get(TokenFilter.class);
        } catch (IllegalStateException ignored) {
        }
        return null;
    }

    static class LoginData {
        String token;
    }

    public LoginData self() {
        LoginData loginData = new LoginData();
        loginData.token = tokenFilter != null ? tokenFilter.generateToken(request) : "";
        return loginData;
    }

    public Renderer addTorrent() throws IOException {
        Request<?,?> request = this.request;
        String link = request.entry("url").asString();
        String folder = request.entry("folder", MultiPartEntry::asString);
        if (folder != null && !appConfig.folders.contains(folder)) {
            response.setStatus(Status._403);
            return renderString("");
        }

        boolean autostart = With.map(request.entry("autostart"), it -> it != null && "on".equals(it.asString()));
        MultiPartEntry entry = request.entry("file");
        byte[] file = entry == null ? new byte[0] : entry.asBytes();

        if (file.length != 0) {
            File tempFile = File.createTempFile("jrt", "torrent");
            tempFile.deleteOnExit();
            FileTools.bytes(tempFile, file);
            torrentClientService.load(tempFile.getAbsolutePath(), autostart, folder);
        } else if (!link.isEmpty()) {
            torrentClientService.load(link, autostart, folder);
        } else {
            throw new IllegalArgumentException("link and file are empty");
        }
        return renderJson("{}");
    }
}
