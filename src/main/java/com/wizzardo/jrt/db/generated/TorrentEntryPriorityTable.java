package com.wizzardo.jrt.db.generated;
import com.wizzardo.tools.sql.query.*;

import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import com.wizzardo.jrt.FilePriority;
public class TorrentEntryPriorityTable extends Table {

    private TorrentEntryPriorityTable(String name, String alias) {
        super(name, alias);
    }

    public TorrentEntryPriorityTable as(String alias) {
        return new TorrentEntryPriorityTable(name, alias);
    }

    public final static TorrentEntryPriorityTable INSTANCE = new TorrentEntryPriorityTable("torrent_entry_priority", null);

    public final Field.LongField ID = new Field.LongField(this, "id");
    public final Field.TimestampField DATE_CREATED = new Field.TimestampField(this, "date_created");
    public final Field.TimestampField DATE_UPDATED = new Field.TimestampField(this, "date_updated");
    public final Field.LongField TORRENT_INFO_ID = new Field.LongField(this, "torrent_info_id");
    public final Field.StringField PATH = new Field.StringField(this, "path");
    public final Field.EnumField<FilePriority> PRIORITY = new Field.EnumField<FilePriority>(this, "priority");

    public final List<Field> FIELDS  = Collections.unmodifiableList(Arrays.asList(ID, DATE_CREATED, DATE_UPDATED, TORRENT_INFO_ID, PATH, PRIORITY));
    public List<Field> getFields() {
        return FIELDS;
    }
}