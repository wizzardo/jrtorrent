package com.wizzardo.jrt.db.generated;
import com.wizzardo.tools.sql.query.*;

import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import com.wizzardo.jrt.TorrentInfo.Status;
public class TorrentInfoTable extends Table {

    private TorrentInfoTable(String name, String alias) {
        super(name, alias);
    }

    public TorrentInfoTable as(String alias) {
        return new TorrentInfoTable(name, alias);
    }

    public final static TorrentInfoTable INSTANCE = new TorrentInfoTable("torrent_info", null);

    public final Field.LongField ID = new Field.LongField(this, "id");
    public final Field.TimestampField DATE_CREATED = new Field.TimestampField(this, "date_created");
    public final Field.TimestampField DATE_UPDATED = new Field.TimestampField(this, "date_updated");
    public final Field.StringField NAME = new Field.StringField(this, "name");
    public final Field.StringField HASH = new Field.StringField(this, "hash");
    public final Field.LongField SIZE = new Field.LongField(this, "size");
    public final Field.LongField DOWNLOADED = new Field.LongField(this, "downloaded");
    public final Field.LongField UPLOADED = new Field.LongField(this, "uploaded");
    public final Field.EnumField<Status> STATUS = new Field.EnumField<Status>(this, "status");
    public final Field.LongField PIECES_COMPLETE = new Field.LongField(this, "pieces_complete");
    public final Field.LongField PIECES_COUNT = new Field.LongField(this, "pieces_count");
    public final Field.StringField FOLDER = new Field.StringField(this, "folder");

    public final List<Field> FIELDS  = Collections.unmodifiableList(Arrays.asList(ID, DATE_CREATED, DATE_UPDATED, NAME, HASH, SIZE, DOWNLOADED, UPLOADED, STATUS, PIECES_COMPLETE, PIECES_COUNT, FOLDER));
    public List<Field> getFields() {
        return FIELDS;
    }
}