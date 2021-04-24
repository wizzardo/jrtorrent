create table torrent_entry_priority
(
    id              IDENTITY,
    date_created    TIMESTAMP,
    date_updated    TIMESTAMP,
    torrent_info_id BIGINT NOT NULL,
    path            varchar(1024),
    priority        varchar(8),
    CONSTRAINT "fk_torrent_entry_priority_torrent_info" FOREIGN KEY (torrent_info_id) REFERENCES torrent_info (id) ON DELETE CASCADE
);
