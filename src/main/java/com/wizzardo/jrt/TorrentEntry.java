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

    public final TorrentEntry parent;
    public final String name;

    public TorrentEntry(String name, TorrentEntry parent) {
        this.name = name;
        this.parent = parent;
    }

    TorrentEntry() {
        name = null;
        parent = null;
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
            entry = new TorrentEntry(name, this);
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

    public void setPriority(FilePriority priority) {
        this.priority = priority;
    }

    public int getChunksCount() {
        return chunksCount;
    }

    public void setChunksCount(int chunksCount) {
        this.chunksCount = chunksCount;
    }

    public int getChunksCompleted() {
        return chunksCompleted;
    }

    public void setChunksCompleted(int chunksCompleted) {
        this.chunksCompleted = chunksCompleted;
    }

    public Map<String, TorrentEntry> getChildren() {
        return children;
    }
}
