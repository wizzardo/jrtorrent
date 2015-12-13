package com.wizzardo.jrt;

import com.wizzardo.http.websocket.DefaultWebSocketHandler;
import com.wizzardo.http.websocket.Message;
import com.wizzardo.tools.json.JsonObject;
import com.wizzardo.tools.json.JsonTools;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by wizzardo on 08.12.15.
 */
public class AppWebSocketHandler extends DefaultWebSocketHandler {

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
    public void onMessage(WebSocketListener listener, Message message) {
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

    protected interface CommandHandler {
        void handle(WebSocketListener listener, JsonObject json);
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
