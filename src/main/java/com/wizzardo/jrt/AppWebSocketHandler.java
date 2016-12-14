package com.wizzardo.jrt;

import com.wizzardo.http.HttpConnection;
import com.wizzardo.http.websocket.DefaultWebSocketHandler;
import com.wizzardo.http.websocket.Message;
import com.wizzardo.http.websocket.WebSocketHandler;
import com.wizzardo.tools.collections.flow.Flow;
import com.wizzardo.tools.json.JsonObject;
import com.wizzardo.tools.json.JsonTools;
import com.wizzardo.tools.misc.Unchecked;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by wizzardo on 08.12.15.
 */
public class AppWebSocketHandler extends DefaultWebSocketHandler<AppWebSocketHandler.PingableListener> {

    protected Map<String, CommandHandler> handlers = new ConcurrentHashMap<>();
    protected TorrentClientService rtorrentClientService;

    @Override
    public String name() {
        return "ws";
    }

    public AppWebSocketHandler() {
        handlers.put("list", (listener, json) -> sendMessage(listener, new ListResponse(rtorrentClientService.list())));

        handlers.put("loadTree", (listener, json) -> {
            String hash = json.getAsJsonObject("args").getAsString("hash");
            sendMessage(listener, new TreeResponse(rtorrentClientService.entries(hash), hash));
        });

        handlers.put("start", (listener, json) -> {
            String hash = json.getAsJsonObject("args").getAsString("hash");
            rtorrentClientService.start(hash);
        });

        handlers.put("stop", (listener, json) -> {
            String hash = json.getAsJsonObject("args").getAsString("hash");
            rtorrentClientService.stop(hash);
        });

        handlers.put("delete", (listener, json) -> {
            String hash = json.getAsJsonObject("args").getAsString("hash");
            boolean withData = json.getAsJsonObject("args").getAsBoolean("withData", Boolean.FALSE);
            rtorrentClientService.delete(hash, withData);
        });

        handlers.put("setPriority", (listener, json) -> {
            JsonObject args = json.getAsJsonObject("args");
            String hash = args.getAsString("hash");
            String path = args.getAsString("path");
            FilePriority priority = FilePriority.valueOf(args.getAsString("priority"));
            rtorrentClientService.setPriority(hash, path, priority);
            sendMessage(listener, new JsonObject()
                    .append("command", "callback")
                    .append("callbackId", args.getAsString("callbackId"))
                    .append("result", "ok")
            );
        });

        handlers.put("ping", (listener, json) -> {
            listener.update();
        });
    }

    @Override
    public void onConnect(PingableListener listener) {
        if (listeners.isEmpty())
            rtorrentClientService.resumeUpdater();
        super.onConnect(listener);
        System.out.println("onConnect. listeners: " + listeners.size());
    }

    @Override
    public void onDisconnect(PingableListener listener) {
        super.onDisconnect(listener);
        if (listeners.isEmpty())
            rtorrentClientService.pauseUpdater();
        System.out.println("onDisconnect. listeners: " + listeners.size());
    }

    @Override
    public void onMessage(PingableListener listener, Message message) {
//        System.out.println(message.asString());
        JsonObject json = JsonTools.parse(message.asString()).asJsonObject();
        CommandHandler handler = handlers.get(json.getAsString("command"));
        if (handler != null)
            handler.handle(listener, json);
        else
            System.out.println("unknown command: " + message.asString());
    }

    public void broadcast(Response response) {
//        System.out.println("broadcast: "+json);
        broadcast(JsonTools.serialize(response));
    }

    public void sendMessage(WebSocketListener listener, String message) {
        System.out.println("send: " + message);
        listener.sendMessage(new Message(message));
    }

    public void sendMessage(WebSocketListener listener, JsonObject json) {
        sendMessage(listener, json.toString());
    }

    public void sendMessage(WebSocketListener listener, Response response) {
        sendMessage(listener, JsonTools.serialize(response));
    }

    public void onUpdate(TorrentInfo ti) {
        broadcast(new GenericTorrentInfoResponse("update", ti));
    }

    public void onAdd(TorrentInfo ti) {
        broadcast(new GenericTorrentInfoResponse("add", ti));
    }

    public void onRemove(TorrentInfo ti) {
        broadcast(new GenericTorrentInfoResponse("remove", ti));
    }

    public void updateDiskStatus(long usableSpace) {
        broadcast(new DiscStatusResponse(usableSpace));
    }

    public void checkConnections() {
        for (PingableListener listener : listeners) {
            if (!listener.isValid())
                listener.close();
        }
    }

    @Override
    protected PingableListener createListener(HttpConnection connection, WebSocketHandler handler) {
        return new PingableListener(connection, handler);
    }

    protected interface CommandHandler {
        void handle(PingableListener listener, JsonObject json);
    }

    public static class PingableListener extends WebSocketHandler.WebSocketListener {
        long lastPing;

        private PingableListener(HttpConnection connection, WebSocketHandler webSocketHandler) {
            super(connection, webSocketHandler);
            update();
        }

        public void update() {
            lastPing = System.currentTimeMillis();
        }

        @Override
        public synchronized void sendMessage(Message message) {
            if (!isValid()) {
                Unchecked.ignore(() -> connection.close());
                return;
            }
            super.sendMessage(message);
        }

        public boolean isValid() {
            return System.currentTimeMillis() - lastPing <= 60_000;
        }
    }

    static class ListResponse extends Response {
        final List<TorrentInfoSerialized> torrents;

        ListResponse(List<TorrentInfo> infos) {
            super("list");
            torrents = Flow.of(infos)
                    .map(AppWebSocketHandler::toSerializedView)
                    .collect(new ArrayList<>(infos.size()))
                    .get();
        }
    }

    static class GenericTorrentInfoResponse extends Response {
        TorrentInfoSerialized torrent;

        GenericTorrentInfoResponse(String command, TorrentInfo ti) {
            super(command);
            torrent = toSerializedView(ti);
        }
    }

    static class DiscStatusResponse extends Response {
        final long free;

        DiscStatusResponse(long free) {
            super("updateDiskStatus");
            this.free = free;
        }
    }

    static class TorrentInfoSerialized {
        String name;
        String hash;
        long size;
        String status;
        long d;
        long ds;
        long u;
        long us;
        long s;
        long p;
        long st;
        long pt;
        float progress;
    }

    private static TorrentInfoSerialized toSerializedView(TorrentInfo it) {
        TorrentInfoSerialized s = new TorrentInfoSerialized();
        s.name = it.getName();
        s.hash = it.getHash();
        s.size = it.getSize();
        s.status = it.getStatus().name();
        s.d = it.getDownloaded();
        s.ds = it.getDownloadSpeed();
        s.u = it.getUploaded();
        s.us = it.getUploadSpeed();
        s.s = it.getSeeds();
        s.p = it.getPeers();
        s.st = it.getTotalSeeds();
        s.pt = it.getTotalPeers();
        s.progress = it.getDownloaded() * 100f / it.getSize();
        return s;
    }

    static class TreeResponse extends Response {
        final Collection<TorrentEntry> tree;
        final String hash;

        TreeResponse(Collection<TorrentEntry> tree, String hash) {
            super("tree");
            this.tree = tree;
            this.hash = hash;
        }
    }

    static class Response {
        final String command;

        Response(String command) {
            this.command = command;
        }
    }
}
