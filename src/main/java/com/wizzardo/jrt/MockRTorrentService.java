package com.wizzardo.jrt;

import com.wizzardo.epoll.ByteBufferProvider;
import com.wizzardo.epoll.ByteBufferWrapper;
import com.wizzardo.http.framework.di.Service;
import com.wizzardo.tools.misc.With;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.wizzardo.tools.misc.With.with;

/**
 * Created by wizzardo on 07.12.15.
 */
public class MockRTorrentService extends RTorrentService {
    volatile List<TorrentInfo> list;
    AppWebSocketHandler appWebSocketHandler;

    public MockRTorrentService() {
        List<TorrentInfo> l = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final int n = i;
            l.add(with(new TorrentInfo(), ti -> {
                ti.setName("test torrent " + n);
                ti.setHash("hash_" + n);
                ti.setSize(100 * (n + 1));
                ti.setDownloaded(0);
                ti.setDownloadSpeed(1);
                ti.setUploaded(10 * n);
                ti.setUploadSpeed(0);
                ti.setTotalPeers(10 * n);
                ti.setTotalSeeds(10 * n);
                ti.setPeers(1 + n);
                ti.setSeeds(1 + n);
                ti.setStatus(TorrentInfo.Status.DOWNLOADING);
            }));
        }
        list = l;
        Thread thread = new Updater(() -> {
            try {
                Thread.sleep(1000); // workaround to wait until websocket handler will be setted
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            while (true) {
                for (TorrentInfo info : list) {
//                    System.out.println("update torrent, set downloaded to " + (info.getDownloaded() == info.getSize() ? 0 : info.getDownloaded() + 1));
                    info.setDownloaded(info.getDownloaded() == info.getSize() ? 0 : info.getDownloaded() + 1);
                    appWebSocketHandler.onUpdate(info);
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

    public List<TorrentInfo> list() {
        return list;
    }

    public Collection<TorrentEntry> entries(TorrentInfo ti) {
        return Collections.emptyList();
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
