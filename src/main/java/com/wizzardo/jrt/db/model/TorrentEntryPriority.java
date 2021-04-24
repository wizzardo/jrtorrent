package com.wizzardo.jrt.db.model;

import com.wizzardo.jrt.FilePriority;

import java.sql.Timestamp;

public class TorrentEntryPriority {
    public long id;
    public Timestamp dateCreated;
    public Timestamp dateUpdated;
    public long torrentInfoId;
    public String path;
    public FilePriority priority;

    public TorrentEntryPriority() {
    }

    public TorrentEntryPriority(Timestamp dateCreated, Timestamp dateUpdated, long torrentInfoId, String path, FilePriority priority) {
        this.dateCreated = dateCreated;
        this.dateUpdated = dateUpdated;
        this.torrentInfoId = torrentInfoId;
        this.path = path;
        this.priority = priority;
    }
}
