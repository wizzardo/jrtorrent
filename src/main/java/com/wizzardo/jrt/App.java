package com.wizzardo.jrt;

import com.wizzardo.http.FileTreeHandler;
import com.wizzardo.http.MultipartHandler;
import com.wizzardo.http.RestHandler;
import com.wizzardo.http.filter.GzipFilter;
import com.wizzardo.http.framework.ControllerHandler;
import com.wizzardo.http.framework.WebApplication;
import com.wizzardo.http.framework.di.DependencyFactory;
import com.wizzardo.http.framework.di.SingletonDependency;
import com.wizzardo.http.framework.message.MessageBundle;
import com.wizzardo.jmx.GcStatsRegistrar;

/**
 * Created by wizzardo on 07.12.15.
 */
public class App {
    final WebApplication server;

    public App(String[] args) {
        server = new WebApplication(args);
        server.onSetup(app -> {
            DependencyFactory.get(MessageBundle.class).load("messages");
//            DependencyFactory.get().register(RTorrentService.class, new SingletonDependency<>(MockRTorrentService.class));

            String downloads = app.getConfig().config("jrt").get("downloads", "./");

            app.getUrlMapping()
                    .append("/", AppController.class, "index")
                    .append("/addTorrent", new MultipartHandler(new ControllerHandler<>(AppController.class, "addTorrent")))
                    .append("/downloads/*", new RestHandler("downloads")
                            .get(new FileTreeHandler(downloads, "/downloads")
                                    .setShowFolder(false)))
                    .append("/ws", "ws", AppWebSocketHandler.class)
                    .append("/tags.js", AppController.class, "tags")
            ;
            app.getFiltersMapping()
                    .addAfter("/tags.js", new GzipFilter())
            ;
        });
        server.start();
        GcStatsRegistrar.registerBeans();
    }

    public static void main(String[] args) {
        new App(args);
    }
}
