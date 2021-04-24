package com.wizzardo.jrt.db.model;

import java.sql.Timestamp;

public class TorrentBitfield {
    public long id;
    public Timestamp dateCreated;
    public Timestamp dateUpdated;
    public long torrentInfoId;
    public byte[] data;

    public TorrentBitfield() {
    }

    public TorrentBitfield(Timestamp dateCreated, Timestamp dateUpdated, long torrentInfoId, byte[] data) {
        this.dateCreated = dateCreated;
        this.dateUpdated = dateUpdated;
        this.torrentInfoId = torrentInfoId;
        this.data = data;
    }

}
