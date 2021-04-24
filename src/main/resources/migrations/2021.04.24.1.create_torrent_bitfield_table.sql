create table torrent_bitfield
(
    id              IDENTITY,
    date_created    TIMESTAMP,
    date_updated    TIMESTAMP,
    torrent_info_id BIGINT NOT NULL,
    data            BYTEA,
    CONSTRAINT "fk_torrent_bitfield_torrent_info" FOREIGN KEY (torrent_info_id) REFERENCES torrent_info (id) ON DELETE CASCADE
);
