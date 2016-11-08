package com.wizzardo.jrt;

import java.util.Collection;
import java.util.List;

/**
 * Created by wizzardo on 04/09/16.
 */
public interface TorrentClientService {
    List<TorrentInfo> list();

    Collection<TorrentEntry> entries(TorrentInfo ti);

    Collection<TorrentEntry> entries(String hash);

    void load(String torrent);

    void load(String torrent, boolean autostart);

    void start(String torrent);

    void stop(String torrent);

    void delete(String torrent, boolean withData);

    void setPriority(String hash, String path, FilePriority priority);

    void pauseUpdater();

    void resumeUpdater();
}
