package com.wizzardo.jrt;

import com.wizzardo.http.HttpConnection;
import com.wizzardo.http.websocket.DefaultWebSocketHandler;
import com.wizzardo.http.websocket.Message;
import com.wizzardo.http.websocket.WebSocketHandler;
import com.wizzardo.tools.json.JsonObject;
import com.wizzardo.tools.json.JsonTools;
import com.wizzardo.tools.misc.Unchecked;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by wizzardo on 08.12.15.
 */
public class AppWebSocketHandler extends DefaultWebSocketHandler<AppWebSocketHandler.PingableListener> {

    protected Map<String, CommandHandler> handlers = new ConcurrentHashMap<>();
    protected RTorrentService rtorrentService;

    public AppWebSocketHandler() {
        handlers.put("list", (listener, json) -> sendMessage(listener, new JsonObject()
                .append("command", "list")
                .append("torrents", rtorrentService.list().stream()
                        .map((it) -> toJson(it)).collect(Collectors.toList()))));


        handlers.put("loadTree", (listener, json) -> {
            String hash = json.getAsJsonObject("args").getAsString("hash");
            sendMessage(listener, new TreeResponse(rtorrentService.entries(hash), hash));
        });

        handlers.put("start", (listener, json) -> {
            String hash = json.getAsJsonObject("args").getAsString("hash");
            rtorrentService.start(hash);
        });

        handlers.put("stop", (listener, json) -> {
            String hash = json.getAsJsonObject("args").getAsString("hash");
            rtorrentService.stop(hash);
        });

        handlers.put("delete", (listener, json) -> {
            String hash = json.getAsJsonObject("args").getAsString("hash");
            boolean withData = json.getAsJsonObject("args").getAsBoolean("withData", Boolean.FALSE);
            rtorrentService.delete(hash, withData);
        });

        handlers.put("setPriority", (listener, json) -> {
            JsonObject args = json.getAsJsonObject("args");
            String hash = args.getAsString("hash");
            String path = args.getAsString("path");
            FilePriority priority = FilePriority.valueOf(args.getAsString("priority"));
            rtorrentService.setPriority(hash, path, priority);
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
            rtorrentService.resumeUpdater();
        super.onConnect(listener);
        System.out.println("onConnect. listeners: " + listeners.size());
    }

    @Override
    public void onDisconnect(PingableListener listener) {
        super.onDisconnect(listener);
        if (listeners.isEmpty())
            rtorrentService.pauseUpdater();
        System.out.println("onDisconnect. listeners: " + listeners.size());
    }

    private JsonObject toJson(TorrentInfo ti) {
        return new JsonObject()
                .append("name", ti.getName())
                .append("hash", ti.getHash())
                .append("size", ti.getSize())
                .append("status", ti.getStatus())
                .append("d", ti.getDownloaded())
                .append("ds", ti.getDownloadSpeed())
                .append("u", ti.getUploaded())
                .append("us", ti.getUploadSpeed())
                .append("s", ti.getSeeds())
                .append("p", ti.getPeers())
                .append("st", ti.getTotalSeeds())
                .append("pt", ti.getTotalPeers())
                .append("progress", ti.getDownloaded() * 100f / ti.getSize());
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

    public void broadcast(JsonObject json) {
//        System.out.println("broadcast: "+json);
        broadcast(json.toString());
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
        JsonObject json = new JsonObject()
                .append("command", "update")
                .append("torrent", toJson(ti));

        broadcast(json);
    }

    public void onAdd(TorrentInfo ti) {
        JsonObject json = new JsonObject()
                .append("command", "add")
                .append("torrent", toJson(ti));

        broadcast(json);
    }

    public void onRemove(TorrentInfo ti) {
        JsonObject json = new JsonObject()
                .append("command", "remove")
                .append("torrent", toJson(ti));

        broadcast(json);
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
                close();
                return;
            }
            super.sendMessage(message);
        }

        public boolean isValid() {
            return System.currentTimeMillis() - lastPing <= 60_000;
        }
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
