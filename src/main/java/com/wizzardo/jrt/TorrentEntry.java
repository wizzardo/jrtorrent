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
    private long sizeBytes;
    private int id = -1;
    private int[] pieces;

    public final String name;

    public TorrentEntry(String name) {
        this.name = name;
    }

    public TorrentEntry(String name, Map<String, TorrentEntry> children) {
        this.name = name;
        this.children = children;
        isFolder = true;
    }

    public TorrentEntry() {
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

    public TorrentEntry get(String name) {
        if (children == null) {
            return null;
        } else
            return children.get(name);
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

    public void setPieces(int[] pieces) {
        this.pieces = pieces;
    }

    public int[] getPieces() {
        return pieces;
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

    public void setId(int id) {
        this.id = id;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }
}
