package com.wizzardo.jrt;

import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import com.wizzardo.http.FileTreeHandler;
import com.wizzardo.http.MultipartHandler;
import com.wizzardo.http.RestHandler;
import com.wizzardo.http.filter.GzipFilter;
import com.wizzardo.http.framework.ControllerHandler;
import com.wizzardo.http.framework.WebApplication;
import com.wizzardo.http.framework.di.DependencyFactory;
import com.wizzardo.http.framework.di.SingletonDependency;
import com.wizzardo.http.framework.message.MessageBundle;
import com.wizzardo.metrics.DatadogClient;
import com.wizzardo.metrics.JvmMonitoring;
import com.wizzardo.metrics.Recorder;

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
                    .append("/ws", AppWebSocketHandler.class)
                    .append("/tags.js", AppController.class, "tags")
            ;
            app.getFiltersMapping()
                    .addAfter("/tags.js", new GzipFilter())
            ;

            DatadogConfig datadogConfig = DependencyFactory.get(DatadogConfig.class);
            if (datadogConfig.enabled) {
                StatsDClient statsDClient = new NonBlockingStatsDClient(datadogConfig.prefix, datadogConfig.hostname, datadogConfig.port, "app:jrt");
                Recorder recorder = new Recorder(new DatadogClient(statsDClient));
                DependencyFactory.get().register(Recorder.class, new SingletonDependency<>(recorder));

                JvmMonitoring jvmMonitoring = new JvmMonitoring(recorder);
                jvmMonitoring.init();
                DependencyFactory.get().register(JvmMonitoring.class, new SingletonDependency<>(jvmMonitoring));

            }
        });
        server.start();
    }

    public static void main(String[] args) {
        new App(args);
    }
}
