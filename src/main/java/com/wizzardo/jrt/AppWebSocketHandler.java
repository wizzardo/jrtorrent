package com.wizzardo.jrt;

import com.wizzardo.epoll.ByteBufferProvider;
import com.wizzardo.epoll.ByteBufferWrapper;
import com.wizzardo.http.HttpConnection;
import com.wizzardo.http.framework.di.DependencyFactory;
import com.wizzardo.http.websocket.DefaultWebSocketHandler;
import com.wizzardo.http.websocket.Message;
import com.wizzardo.http.websocket.WebSocketHandler;
import com.wizzardo.metrics.JvmMonitoring;
import com.wizzardo.metrics.Recorder;
import com.wizzardo.tools.collections.flow.Flow;
import com.wizzardo.tools.json.JsonObject;
import com.wizzardo.tools.json.JsonTools;
import com.wizzardo.tools.misc.ExceptionDrivenStringBuilder;
import com.wizzardo.tools.misc.Unchecked;
import com.wizzardo.tools.misc.With;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by wizzardo on 08.12.15.
 */
public class AppWebSocketHandler<L extends AppWebSocketHandler.PingableListener> extends DefaultWebSocketHandler<L> {

    protected TorrentClientService rtorrentClientService;
    protected StorageStatusService storageStatusService;
    protected Recorder recorder;

    protected Map<String, Map.Entry<Class<? extends CommandPojo>, CommandHandler<? extends L, ? extends CommandPojo>>>
            handlers = new ConcurrentHashMap<>(16, 1f);

    protected Reader reader = (clazz, bytes, offset, length) -> recorder.rec(() -> JsonTools.parse(bytes, offset, length, clazz),
            Recorder.Tags.of("method", "parse", "command", clazz.getSimpleName())
    );
    protected ErrorHandler errorHandler = e -> e.printStackTrace();
    protected Sender sender = With.with(new Sender(errorHandler), s -> s.start());

    static class Sender extends Thread implements ByteBufferProvider {
        protected final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(100);
        protected final ByteBufferWrapper byteBufferWrapper = new ByteBufferWrapper(1024 * 50);
        protected final ErrorHandler errorHandler;

        Sender(ErrorHandler errorHandler) {
            setName("WebsocketSender");
            setDaemon(true);
            this.errorHandler = errorHandler;
        }

        @Override
        public ByteBufferWrapper getBuffer() {
            return byteBufferWrapper;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    queue.take().run();
                } catch (Exception e) {
                    errorHandler.onError(e);
                }
            }
        }

        public void addTask(Runnable o) {
            while (true)
                try {
                    while (!queue.offer(o, 1000, TimeUnit.SECONDS)) {
                        System.out.println("waiting for queue");
                    }
                    return;
                } catch (InterruptedException ignored) {
                }
        }
    }


    public interface ErrorHandler {
        void onError(Exception e);
    }

    public interface Reader {
        Object read(Class<?> clazz, byte[] bytes, int offset, int length);
    }

    public interface CommandPojo {
    }

    public interface CommandHandler<T, C extends CommandPojo> {
        void handle(T client, C command);
    }

    public <C extends CommandPojo> void addHandler(Class<C> commandClass, CommandHandler<? extends L, C> handler) {
        AbstractMap.SimpleEntry<Class<? extends CommandPojo>, CommandHandler<? extends L, ? extends CommandPojo>> entry = new AbstractMap.SimpleEntry<>(commandClass, handler);
        handlers.put(commandClass.getSimpleName(), entry);
    }

    public void onMessage(L listener, Message message) {
        try {
            byte[] bytes = message.asBytes();

            int[] holder = new int[1];
            int position = readInt(holder, bytes, 0, bytes.length);
            int nameLength = holder[0];
            String commandName;
            if (nameLength != -1) {
                commandName = new String(bytes, position, nameLength);
            } else {
                position = 0;
                nameLength = indexOf((byte) '{', bytes, position, bytes.length);
                commandName = new String(bytes, position, nameLength);
            }
            int offset = position + nameLength;
            Map.Entry<Class<? extends CommandPojo>, CommandHandler<? extends L, ? extends CommandPojo>> commandHandlerPair = handlers.get(commandName);
            if (commandHandlerPair == null)
                throw new IllegalArgumentException("Unknown command: " + commandName);

            CommandHandler<L, CommandPojo> handler = (CommandHandler<L, CommandPojo>) commandHandlerPair.getValue();
            Class<? extends CommandPojo> commandClass = commandHandlerPair.getKey();
            CommandPojo command = (CommandPojo) reader.read(commandClass, bytes, offset, bytes.length - offset);

            recorder.rec(() -> handler.handle(listener, command), Recorder.Tags.of("method", "handleCommand", "command", commandName));
        } catch (Exception e) {
            onError(e);
        }
    }

    protected void onError(Exception e) {
        errorHandler.onError(e);
    }

    protected static int indexOf(byte b, byte[] bytes, int offset, int limit) {
        for (int i = offset; i < limit; i++) {
            if (bytes[i] == b)
                return i;
        }
        return -1;
    }

    protected static int readInt(int[] holder, byte[] bytes, int offset, int limit) {
        int value = 0;
        int i = offset;
        while (i < limit) {
            byte b = bytes[i];
            if (b >= '0' && b <= '9') {
                value = value * 10 + (b - '0');
            } else {
                if (i == offset)
                    holder[0] = -1;
                else
                    holder[0] = value;
                return i;
            }
            i++;
        }

        holder[0] = value;
        return limit;
    }

    static class AppInfo implements CommandPojo {
    }

    static class Ping implements CommandPojo {
    }

    static class GetList implements CommandPojo {
    }

    static class GetTorrentFileTree implements CommandPojo {
        String hash;
    }

    static class StartTorrent implements CommandPojo {
        String hash;
    }

    static class StopTorrent implements CommandPojo {
        String hash;
    }

    static class DeleteTorrent implements CommandPojo {
        String hash;
        boolean withData;
    }

    static class SetFilePriority implements CommandPojo {
        String hash;
        String path;
        FilePriority priority;
    }

    @Override
    public String name() {
        return "ws";
    }

    public AppWebSocketHandler() {

        addHandler(GetList.class, (l, c) -> {
            sendMessage(l, new ListResponse(rtorrentClientService.list()));
        });
        addHandler(GetTorrentFileTree.class, (l, c) -> {
            sendMessage(l, new FileTreeResponse(rtorrentClientService.entries(c.hash), c.hash, rtorrentClientService.getEncodedBitfield(c.hash)));
        });

        addHandler(StartTorrent.class, (l, c) -> {
            rtorrentClientService.start(c.hash);
        });
        addHandler(StopTorrent.class, (l, c) -> {
            rtorrentClientService.stop(c.hash);
        });
        addHandler(DeleteTorrent.class, (l, c) -> {
            rtorrentClientService.delete(c.hash, c.withData);
        });

        addHandler(SetFilePriority.class, (l, c) -> {
            rtorrentClientService.setPriority(c.hash, c.path, c.priority);
//            sendMessage(listener, new JsonObject()
//                    .append("command", "callback")
//                    .append("callbackId", args.getAsString("callbackId"))
//                    .append("result", "ok")
//            );
        });

        addHandler(Ping.class, (l, c) -> {
            l.update();
        });

        JvmMonitoring jvmMonitoring = DependencyFactory.get(JvmMonitoring.class);
        if (jvmMonitoring != null) {
            jvmMonitoring.add("ws.connections", new JvmMonitoring.Recordable() {
                @Override
                public void record(Recorder recorder) {
                    recorder.gauge("ws.connections", listeners.size());
                }

                @Override
                public boolean isValid() {
                    return true;
                }
            });
        }
    }

    @Override
    public void onConnect(L listener) {
        if (listeners.isEmpty()) {
            rtorrentClientService.resumeUpdater();
            storageStatusService.unpause();
        }
        super.onConnect(listener);
        sendMessage(listener, new AppInfo());
        System.out.println("onConnect. listeners: " + listeners.size());
    }

    @Override
    public void onDisconnect(L listener) {
        super.onDisconnect(listener);
        if (listeners.isEmpty()) {
            rtorrentClientService.pauseUpdater();
            storageStatusService.pause();
        }
        System.out.println("onDisconnect. listeners: " + listeners.size());
    }

    public void broadcast(Object response) {
        broadcast(serialize(response));
    }

    public void addBroadcastTask(Object response) {
        byte[] serialize = serialize(response);
        sender.addTask(() -> broadcast(serialize));
    }

    public byte[] serialize(Object o) {
        return serialize(o.getClass().getSimpleName(), o);
    }

    public byte[] serialize(String name, Object o) {
        return recorder.rec(() -> ExceptionDrivenStringBuilder.withBuilder(builder -> {
                    builder.append(name);
                    JsonTools.serialize(o, builder);
                    return builder.toBytes();
                }),
                Recorder.Tags.of("method", "serialize", "command", name)
        );
    }

    public void sendMessage(WebSocketListener listener, String message) {
//        System.out.println("send: " + message);
        listener.sendMessage(new Message(message));
    }

    public void sendMessage(WebSocketListener listener, byte[] message) {
//        System.out.println("send: " + message);
        listener.sendMessage(new Message(message));
    }

    public void sendMessage(WebSocketListener listener, JsonObject json) {
        sendMessage(listener, json.toString());
    }

    public void sendMessage(WebSocketListener listener, Object response) {
        sendMessage(listener, serialize(response));
    }

    public void onUpdate(TorrentInfo ti) {
        broadcast(toSerializedView(ti, new TorrentUpdated()));
    }

    public void onAdd(TorrentInfo ti) {
        broadcast(toSerializedView(ti, new TorrentAdded()));
    }

    public void onRemove(TorrentInfo ti) {
        broadcast(toSerializedView(ti, new TorrentDeleted()));
    }

    public void updateDiskStatus(long usableSpace) {
        broadcast(new DiskUsage(usableSpace));
    }

    public void checkConnections() {
        for (PingableListener listener : listeners) {
            if (!listener.isValid())
                listener.close();
        }
    }

    @Override
    protected L createListener(HttpConnection connection, WebSocketHandler handler) {
        return (L) new PingableListener(connection, handler);
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

    static class ListResponse {
        final List<TorrentInfoSerialized> torrents;

        ListResponse(List<TorrentInfo> infos) {
            torrents = Flow.of(infos)
                    .map(torrentInfo -> toSerializedView(torrentInfo, new TorrentInfoSerialized()))
                    .collect(new ArrayList<>(infos.size()))
                    .get();
        }
    }

    static public class TorrentUpdated extends TorrentInfoSerialized {
    }

    static public class TorrentDeleted extends TorrentInfoSerialized {
    }

    static public class TorrentAdded extends TorrentInfoSerialized {
    }

    public static class DiskUsage {
        final long free;

        public DiskUsage(long free) {
            this.free = free;
        }
    }

    public static class TorrentInfoSerialized {
        public String name;
        public String folder;
        public String hash;
        public long size;
        public String status;
        public long d;
        public long ds;
        public long u;
        public long us;
        public long s;
        public long p;
        public long st;
        public long pt;
        public float progress;
        public String bitfield;
        public int piecesTotal;
        public int piecesComplete;
    }

    public static TorrentInfoSerialized toSerializedView(TorrentInfo it, TorrentInfoSerialized view) {
        view.name = it.getName();
        view.folder = it.getFolder();
        view.hash = it.getHash();
        view.size = it.getSize();
        view.status = it.getStatus().name();
        view.d = it.getDownloaded();
        view.ds = it.getDownloadSpeed();
        view.u = it.getUploaded();
        view.us = it.getUploadSpeed();
        view.s = it.getSeeds();
        view.p = it.getPeers();
        view.st = it.getTotalSeeds();
        view.pt = it.getTotalPeers();
        view.progress = it.getSize() == 0 ? 0 : it.getDownloaded() * 100f / it.getSize();
        return view;
    }

    static class FileTreeResponse {
        final Collection<TorrentEntry> tree;
        final String hash;
        final String bitfield;

        FileTreeResponse(Collection<TorrentEntry> tree, String hash, String bitfield) {
            this.tree = tree;
            this.hash = hash;
            this.bitfield = bitfield;
        }
    }
}
