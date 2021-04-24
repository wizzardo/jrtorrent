drop table if exists torrent_info;
create table torrent_info
(
    id           IDENTITY,
    date_created TIMESTAMP,
    date_updated TIMESTAMP,
    name         VARCHAR(512),
    hash         CHAR(40),
    size         BIGINT,
    downloaded   BIGINT,
    uploaded     BIGINT,
    status       VARCHAR(32)
);
