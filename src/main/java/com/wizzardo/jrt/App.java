package com.wizzardo.jrt;

import com.wizzardo.http.FileTreeHandler;
import com.wizzardo.http.MultipartHandler;
import com.wizzardo.http.RestHandler;
import com.wizzardo.http.framework.ControllerHandler;
import com.wizzardo.http.framework.Environment;
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

    public App(Environment environment) {
        server = new WebApplication();
        server.onSetup(app -> {
            DependencyFactory.getDependency(MessageBundle.class).load("messages");
//            DependencyFactory.get().register(RTorrentService.class, new SingletonDependency<>(MockRTorrentService.class));

            String downloads = app.getConfig().config("jrt").get("downloads", "./");

            app.getUrlMapping()
                    .append("/", AppController.class, "index")
                    .append("/addTorrent", new MultipartHandler(new ControllerHandler(AppController.class, "addTorrent")))
                    .append("/downloads/*", new RestHandler("downloads").get(new FileTreeHandler(downloads, "/downloads")
                            .setShowFolder(false)))
                    .append("/ws", "ws", DependencyFactory.getDependency(AppWebSocketHandler.class))
            ;
        });
        server.setEnvironment(environment);
        server.start();
        GcStatsRegistrar.registerBeans();
    }

    public static void main(String[] args) {
        Environment environment = args.length == 1 && args[0].startsWith("-env=") ? Environment.parse(args[0].substring(5)) : Environment.DEVELOPMENT;
        new App(environment);
    }
}
