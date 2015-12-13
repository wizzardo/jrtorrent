<torrents>
    <div class="header">
        <span class="th name">NAME</span>
        <span class="th status">STATUS</span>
        <span class="th">SIZE</span>
        <span class="th">DL</span>
        <span class="th">↓SPEED</span>
        <span class="th">UL</span>
        <span class="th">↑SPEED</span>
        <span class="th">PEERS</span>
        <span class="th">SEEDS</span>
    </div>
    <torrent each={opts.torrents}>
    </torrent>

    <style scoped>
        :scope {
            display: block
        }

        .header {
            margin: 20px 10px 0 10px;
            border-bottom: 1px solid gray;
        }

        .header span {
            font-weight: bold;
        }

        .header .th {
            width: 63px;
            display: inline-block;
        }

        .th.name {
            width: 150px;
        }

        .th.status {
            width: 120px;
        }

        @media screen and (max-width: 770px) {
            .header {
                display: none;
            }
        }
    </style>
</torrents>