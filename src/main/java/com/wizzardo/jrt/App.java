package com.wizzardo.jrt;

import com.wizzardo.http.framework.ControllerHandler;
import com.wizzardo.http.framework.WebApplication;
import com.wizzardo.http.framework.di.DependencyFactory;
import com.wizzardo.http.framework.di.SingletonDependency;
import com.wizzardo.http.framework.message.MessageBundle;

/**
 * Created by wizzardo on 07.12.15.
 */
public class App {
    final WebApplication server;

    public App() {
        server = new WebApplication("0.0.0.0", 8084, 4);
        server.setTTL(5 * 60 * 1000);

        DependencyFactory.getDependency(MessageBundle.class).load("messages");
        DependencyFactory.get().register(RTorrentService.class, new SingletonDependency<>(MockRTorrentService.class));

        server.getUrlMapping()
                .append("/", new ControllerHandler(AppController.class, "riotIndex"))
                .append("/ws", DependencyFactory.getDependency(AppWebSocketHandler.class))
        ;

        server.start();
    }

    public static void main(String[] args) {
        new App();
    }
}
