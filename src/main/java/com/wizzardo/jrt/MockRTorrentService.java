package com.wizzardo.jrt;

import com.wizzardo.epoll.ByteBufferProvider;
import com.wizzardo.epoll.ByteBufferWrapper;
import com.wizzardo.tools.collections.lazy.Lazy;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static com.wizzardo.tools.misc.With.with;

/**
 * Created by wizzardo on 07.12.15.
 */
public class MockRTorrentService extends RTorrentService {
    volatile List<TorrentInfo> list = new CopyOnWriteArrayList<>();
    final AtomicInteger counter = new AtomicInteger();
    AppWebSocketHandler appWebSocketHandler;

    public MockRTorrentService() {
        for (int i = 0; i < 10; i++) {
            load("");
        }
        Thread thread = new Updater(() -> {
            try {
                Thread.sleep(1000); // workaround to wait until websocket handler will be setted
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            while (true) {
                for (TorrentInfo ti : list) {
                    if (ti.getStatus() != TorrentInfo.Status.DOWNLOADING)
                        continue;

//                    System.out.println("update torrent, set downloaded to " + (info.getDownloaded() == info.getSize() ? 0 : info.getDownloaded() + 1));
                    if (ti.getDownloaded() == ti.getSize()) {
                        ti.setDownloaded(0);
                        ti.setUploaded(0);
                        ti.setUploadSpeed(1);
                    } else {
                        ti.setDownloaded(ti.getDownloadSpeed());
                        ti.setUploaded(ti.getUploaded() + ti.getUploadSpeed());
                        ti.setUploadSpeed(ti.getUploadSpeed() + 5);
                    }
                    appWebSocketHandler.onUpdate(ti);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private TorrentInfo createTorrent(int n) {
        return with(new TorrentInfo(), ti -> {
            ti.setName("test torrent " + n);
            ti.setHash("hash_" + n);
            ti.setSize(100 * (n + 1));
            ti.setDownloaded(0);
            ti.setDownloadSpeed(1);
            ti.setUploaded(0);
            ti.setUploadSpeed(1);
            ti.setTotalPeers(10 * n);
            ti.setTotalSeeds(10 * n);
            ti.setPeers(1 + n);
            ti.setSeeds(1 + n);
            ti.setStatus(TorrentInfo.Status.DOWNLOADING);
        });
    }

    @Override
    public void delete(String torrent, boolean withData) {
        TorrentInfo info = find(torrent);
        list.remove(info);
        appWebSocketHandler.onRemove(info);
    }

    @Override
    public void load(String torrent) {
        TorrentInfo info = createTorrent(counter.getAndIncrement());
        if (appWebSocketHandler != null)
            appWebSocketHandler.onAdd(info);
        list.add(0, info);
    }

    public void load(String torrent, boolean autostart) {
        load(torrent);
    }

    @Override
    public void start(String torrent) {
        TorrentInfo info = find(torrent);
        info.setDownloadSpeed(1);
        info.setUploadSpeed(1);
        info.setStatus(TorrentInfo.Status.DOWNLOADING);
        appWebSocketHandler.onUpdate(info);
    }

    @Override
    public void stop(String torrent) {
        TorrentInfo info = find(torrent);
        info.setDownloadSpeed(0);
        info.setUploadSpeed(0);
        info.setStatus(TorrentInfo.Status.STOPPED);
        appWebSocketHandler.onUpdate(info);
    }

    public List<TorrentInfo> list() {
        return list;
    }

    public Collection<TorrentEntry> entries(TorrentInfo ti) {
        return entries(ti.getHash());
    }

    @Override
    public Collection<TorrentEntry> entries(String hash) {
        TorrentEntry root = new TorrentEntry();
        int id = 0;
        TorrentEntry folder1 = root.getOrCreate("test folder");
        folder1.setId(id++);
        TorrentEntry subfolder = folder1.getOrCreate("subfolder");
        subfolder.setId(id++);
        subfolder.getOrCreate("file1").setId(id++);
        folder1.getOrCreate("file1").setId(id++);
        folder1.getOrCreate("file2").setId(id++);
        folder1.getOrCreate("file3").setId(id++);
        TorrentEntry folder2 = root.getOrCreate("test folder 2");
        folder2.setId(id++);
        folder2.getOrCreate("file1").setId(id++);
        folder2.getOrCreate("file2").setId(id++);
        folder2.getOrCreate("file3").setId(id++);

        return root.getChildren().values();
    }

    private TorrentInfo find(String torrent) {
        return Lazy.of(list).filter(ti -> ti.getHash().equals(torrent)).first();
    }

    static class Updater extends Thread implements ByteBufferProvider {
        private ByteBufferWrapper buffer = new ByteBufferWrapper(1024 * 50);

        Updater(Runnable runnable) {
            super(runnable);
        }

        @Override
        public ByteBufferWrapper getBuffer() {
            return buffer;
        }
    }
}
