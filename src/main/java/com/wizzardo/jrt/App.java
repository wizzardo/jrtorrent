package com.wizzardo.jrt;

import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import com.wizzardo.http.FileTreeHandler;
import com.wizzardo.http.MultipartHandler;
import com.wizzardo.http.RestHandler;
import com.wizzardo.http.filter.TokenFilter;
import com.wizzardo.http.framework.*;
import com.wizzardo.http.framework.di.DependencyFactory;
import com.wizzardo.http.framework.di.SingletonDependency;
import com.wizzardo.http.framework.message.MessageBundle;
import com.wizzardo.http.framework.template.LocalResourcesTools;
import com.wizzardo.http.framework.template.ResourceTools;
import com.wizzardo.http.request.Header;
import com.wizzardo.http.response.RangeResponseHelper;
import com.wizzardo.jrt.bt.BtService;
import com.wizzardo.metrics.*;
import com.wizzardo.tools.io.FileTools;
import com.wizzardo.tools.misc.Unchecked;

import java.io.File;
import java.net.InetAddress;
import java.util.Collections;

/**
 * Created by wizzardo on 07.12.15.
 */
public class App {
    final WebApplication server;

    public App(String[] args) {
        server = new WebApplication(args);
        server.onSetup(app -> {
            initMonitoring(app);
            DependencyFactory.get(MessageBundle.class).load("messages");
//            DependencyFactory.get().register(TorrentClientService.class, new SingletonDependency<>(BtService.class));
            DependencyFactory.get().register(TorrentClientService.class, new SingletonDependency<>(MockRTorrentService.class));
//            DependencyFactory.get().register(TorrentClientService.class, RTorrentService.class);

            String downloads = app.getConfig().config("jrt").get("downloads", "./");
            TokenFilter tokenFilter = DependencyFactory.get(TokenFilter.class);
            LocalResourcesTools resourceTools = (LocalResourcesTools) DependencyFactory.get(ResourceTools.class);

            app.getUrlMapping()
                    .append("/info", (request, response) -> response.appendHeader(Header.KV_CONTENT_TYPE_APPLICATION_JSON).body("{\"status\":\"OK\"}"))
                    .append("/", (request, response) -> {
                        byte[] html;
                        if (app.getEnvironment() == Environment.PRODUCTION) {
                            html = FileTools.bytes(new File(resourceTools.getUnzippedJarDirectory(), "/public/index.html"));
                        } else {
                            html = FileTools.bytes(resourceTools.getResourceFile("/public/index.html"));
                        }
                        return response.appendHeader(Header.KV_CONTENT_TYPE_HTML_UTF8).body(html);
                    })
                    .append("/users/self", AppController.class, "self")
                    .append("/addTorrent", new RestHandler()
                            .post(new MultipartHandler(new ControllerHandler<>(AppController.class, "addTorrent")))
                    )
                    .append("/zip/*", new ZipHandler(downloads, "zip", "zip"))
                    .append("/m3u/*", new M3UHandler(downloads, "m3u", tokenFilter, "m3u"))
                    .append("/downloads/*", new RestHandler("downloads")
                            .get(new FileTreeHandler(downloads, "/downloads")
                                    .setRangeResponseHelper(new RangeResponseHelper())
                                    .setShowFolder(true)))
                    .append("/ws", AppWebSocketHandler.class)
            ;

            for (String alias : app.getConfig().config("jrt").get("downloadsAliases", Collections.<String>emptyList())) {
                app.getUrlMapping()
                        .append("/" + alias + "/*", new RestHandler()
                                .get(new FileTreeHandler(downloads, "/" + alias)
                                        .setRangeResponseHelper(new RangeResponseHelper())
                                        .setShowFolder(true)));
            }

            for (String folder : app.getConfig().config("jrt").get("folders", Collections.<String>emptyList())) {
                new File(downloads, folder).mkdirs();
                app.getUrlMapping()
                        .append("/" + folder + "/*", new RestHandler()
                                .get(new FileTreeHandler(downloads + "/" + folder, "/" + folder)
                                        .setRangeResponseHelper(new RangeResponseHelper())
                                        .setShowFolder(true)));
            }

            if (app.getEnvironment() != Environment.PRODUCTION) {
                app.getFiltersMapping().addAfter("/*", (request, response) -> {
                    String origin = request.header(Header.KEY_ORIGIN);
                    if (origin != null && response.header("Access-Control-Allow-Origin") == null) {
                        response.header("Access-Control-Allow-Credentials", "true");
                        response.header("Access-Control-Allow-Origin", origin);
                    }
                    if (request.header("Access-Control-Request-Headers") != null)
                        response.appendHeader("Access-Control-Allow-Headers", request.header("Access-Control-Request-Headers"));

                    return true;
                });
            }
        });
        server.start();
    }

    protected void initMonitoring(WebApplication app) {
        DatadogConfig datadogConfig = DependencyFactory.get(DatadogConfig.class);
        if (datadogConfig.enabled) {
            String hostName = Unchecked.call(() -> InetAddress.getLocalHost().getHostName());
            StatsDClient statsDClient = new NonBlockingStatsDClient(datadogConfig.prefix, datadogConfig.hostname, datadogConfig.port, "app:jrt", "origin:" + hostName);
            Recorder recorder = new Recorder(new DatadogClient(statsDClient));
            DependencyFactory.get().register(Recorder.class, new SingletonDependency<>(recorder));

            JvmMonitoring jvmMonitoring = new JvmMonitoring(recorder);
            jvmMonitoring.setWithCacheMetrics(false);
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
        } else {
            DependencyFactory.get().register(Recorder.class, new SingletonDependency<>(new NoopRecorder()));
        }
    }

    public static void main(String[] args) {
        new App(args);
    }
}
