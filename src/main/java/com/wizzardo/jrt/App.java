package com.wizzardo.jrt;

import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import com.wizzardo.http.FileTreeHandler;
import com.wizzardo.http.MultipartHandler;
import com.wizzardo.http.RestHandler;
import com.wizzardo.http.framework.*;
import com.wizzardo.http.framework.di.DependencyFactory;
import com.wizzardo.http.framework.di.SingletonDependency;
import com.wizzardo.http.framework.message.MessageBundle;
import com.wizzardo.http.framework.template.LocalResourcesTools;
import com.wizzardo.http.framework.template.ResourceTools;
import com.wizzardo.http.response.RangeResponseHelper;
import com.wizzardo.metrics.DatadogClient;
import com.wizzardo.metrics.JvmMonitoring;
import com.wizzardo.metrics.Recorder;
import com.wizzardo.tools.io.FileTools;
import com.wizzardo.tools.misc.Unchecked;

import java.io.File;
import java.net.InetAddress;

/**
 * Created by wizzardo on 07.12.15.
 */
public class App {
    final WebApplication server;

    public App(String[] args) {
        server = new WebApplication(args);
        server.onSetup(app -> {
            DependencyFactory.get(MessageBundle.class).load("messages");
//            DependencyFactory.get().register(TorrentClientService.class, new SingletonDependency<>(MockRTorrentService.class));
            DependencyFactory.get().register(TorrentClientService.class, RTorrentService.class);

            String downloads = app.getConfig().config("jrt").get("downloads", "./");

            app.getUrlMapping()
                    .append("/", AppController.class, "index")
                    .append("/addTorrent", new MultipartHandler(new ControllerHandler<>(AppController.class, "addTorrent")))
                    .append("/zip/*", new ZipHandler(downloads, "zip", "zip"))
                    .append("/downloads/*", new RestHandler("downloads")
                            .get(new FileTreeHandler(downloads, "/downloads")
                                    .setRangeResponseHelper(new RangeResponseHelper())
                                    .setShowFolder(false)))
                    .append("/ws", AppWebSocketHandler.class)
            ;

            LocalResourcesTools resourceTools = (LocalResourcesTools) DependencyFactory.get(ResourceTools.class);
            if (app.getEnvironment() == Environment.PRODUCTION && resourceTools.isJar()) {
                String tags = DependencyFactory.get(AppController.class).tags().render().toString();
                FileTools.text(new File(resourceTools.getUnzippedJarDirectory(), "/public/js/tags.js"), tags);
            } else {
                app.getUrlMapping()
                        .append("/static/js/tags.js", AppController.class, "tags");
            }

            DatadogConfig datadogConfig = DependencyFactory.get(DatadogConfig.class);
            if (datadogConfig.enabled) {
                String hostName = Unchecked.call(() -> InetAddress.getLocalHost().getHostName());
                StatsDClient statsDClient = new NonBlockingStatsDClient(datadogConfig.prefix, datadogConfig.hostname, datadogConfig.port, "app:jrt", "origin:" + hostName);
                Recorder recorder = new Recorder(new DatadogClient(statsDClient));
                DependencyFactory.get().register(Recorder.class, new SingletonDependency<>(recorder));

                JvmMonitoring jvmMonitoring = new JvmMonitoring(recorder);
                jvmMonitoring.init();
                DependencyFactory.get().register(JvmMonitoring.class, new SingletonDependency<>(jvmMonitoring));

                app.getFiltersMapping()
                        .addAfter("/*", (request, response) -> {
                            RequestContext context = RequestContext.get();
                            String controller = context.controller();
                            String action = context.action();
                            RequestHolder requestHolder = context.getRequestHolder();

                            long executeTime = 0;
                            if (requestHolder != null) {
                                executeTime = requestHolder.getExecutionTimeUntilNow() / 1_000_000L;
                            }

                            Recorder.Tags tags = new Recorder.Tags()
                                    .add("controller", String.valueOf(controller))
                                    .add("action", String.valueOf(action))
                                    .add("handler", String.valueOf(context.handler()))
                                    .add("status", String.valueOf(response.status().code));

                            recorder.rec(Recorder.ACTION_DURATION, executeTime, tags);
                            return true;
                        })
                ;
            }
        });
        server.start();
    }

    public static void main(String[] args) {
        new App(args);
    }
}
