create table torrent_binary
(
    id              IDENTITY,
    date_created    TIMESTAMP,
    date_updated    TIMESTAMP,
    torrent_info_id BIGINT NOT NULL,
    data            BYTEA,
    CONSTRAINT "fk_torrent_binary_torrent_info" FOREIGN KEY (torrent_info_id) REFERENCES torrent_info (id) ON DELETE CASCADE
);
