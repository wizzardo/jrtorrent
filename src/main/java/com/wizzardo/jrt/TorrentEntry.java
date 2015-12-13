package com.wizzardo.jrt;

import java.util.Map;
import java.util.TreeMap;

/**
 * Created by wizzardo on 09.10.15.
 */
public class TorrentEntry {
    private boolean isFolder;
    private FilePriority priority;
    private Map<String, TorrentEntry> children;

    private int chunksCount;
    private int chunksCompleted;
    private int id;

    public final String name;

    public TorrentEntry(String name) {
        this.name = name;
    }

    TorrentEntry() {
        name = null;
        isFolder = true;
    }

    public TorrentEntry getOrCreate(String name) {
        TorrentEntry entry = null;
        if (children == null) {
            children = new TreeMap<>();
            this.isFolder = true;
        } else
            entry = children.get(name);

        if (entry == null) {
            entry = new TorrentEntry(name);
            children.put(name, entry);
        }

        return entry;
    }

    public boolean isFolder() {
        return isFolder;
    }

    public FilePriority getPriority() {
        return priority;
    }

    void setPriority(FilePriority priority) {
        this.priority = priority;
    }

    public int getChunksCount() {
        return chunksCount;
    }

    void setChunksCount(int chunksCount) {
        this.chunksCount = chunksCount;
    }

    public int getChunksCompleted() {
        return chunksCompleted;
    }

    void setChunksCompleted(int chunksCompleted) {
        this.chunksCompleted = chunksCompleted;
    }

    public Map<String, TorrentEntry> getChildren() {
        return children;
    }

    public int getId() {
        return id;
    }

    void setId(int id) {
        this.id = id;
    }
}
