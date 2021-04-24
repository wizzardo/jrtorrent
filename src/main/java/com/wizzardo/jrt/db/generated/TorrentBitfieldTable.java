package com.wizzardo.jrt.db.generated;
import com.wizzardo.jrt.db.query.*;

import java.util.Arrays;
import java.util.List;
import java.util.Collections;
public class TorrentBitfieldTable extends Table {

    private TorrentBitfieldTable(String name, String alias) {
        super(name, alias);
    }

    public TorrentBitfieldTable as(String alias) {
        return new TorrentBitfieldTable(name, alias);
    }

    public final static TorrentBitfieldTable INSTANCE = new TorrentBitfieldTable("torrent_bitfield", null);

    public final Field.LongField ID = new Field.LongField(this, "id");
    public final Field.TimestampField DATE_CREATED = new Field.TimestampField(this, "date_created");
    public final Field.TimestampField DATE_UPDATED = new Field.TimestampField(this, "date_updated");
    public final Field.LongField TORRENT_INFO_ID = new Field.LongField(this, "torrent_info_id");
    public final Field.ByteArrayField DATA = new Field.ByteArrayField(this, "data");

    public final List<Field> FIELDS  = Collections.unmodifiableList(Arrays.asList(ID, DATE_CREATED, DATE_UPDATED, TORRENT_INFO_ID, DATA));
    public List<Field> getFields() {
        return FIELDS;
    }
}