package com.wizzardo.jrt.db.model;

import java.sql.Timestamp;

public class TorrentInfo {
    public long id;
    public Timestamp dateCreated;
    public Timestamp dateUpdated;
    public String name;
    public String hash;
    public long size;
    public long downloaded;
    public long uploaded;
    public com.wizzardo.jrt.TorrentInfo.Status status;
    public long piecesComplete;
    public long piecesCount;

    public TorrentInfo() {
    }

    public TorrentInfo(Timestamp dateCreated, Timestamp dateUpdated, String name, String hash, long size, long downloaded, long uploaded, com.wizzardo.jrt.TorrentInfo.Status status, long piecesComplete, long piecesCount) {
        this.dateCreated = dateCreated;
        this.dateUpdated = dateUpdated;
        this.name = name;
        this.hash = hash;
        this.size = size;
        this.downloaded = downloaded;
        this.uploaded = uploaded;
        this.status = status;
        this.piecesComplete = piecesComplete;
        this.piecesCount = piecesCount;
    }

    @Override
    public String toString() {
        return "TorrentInfo{" +
                "id=" + id +
                ", dateCreated=" + dateCreated +
                ", dateUpdated=" + dateUpdated +
                ", name='" + name + '\'' +
                ", hash='" + hash + '\'' +
                ", size=" + size +
                ", downloaded=" + downloaded +
                ", uploaded=" + uploaded +
                ", status=" + status +
                ", piecesComplete=" + piecesComplete +
                ", piecesCount=" + piecesCount +
                '}';
    }
}
