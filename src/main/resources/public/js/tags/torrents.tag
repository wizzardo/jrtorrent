<torrents>
    <div class="header">
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
            padding-left: 44px;
        }

        .header span {
            font-weight: bold;
        }

        .header .th {
            width: 95px;
            display: inline-block;
        }

        .th.status {
            width: 120px;
        }

        @media screen and (max-width: 850px) {
            .header {
                display: none;
            }
        }
    </style>
    <script>
        var that = this;

        that.on('mount', function () {
            obs.on('removeTorrent', function (data) {
                var torrents = that.opts.torrents;
                for (var i = 0; i < torrents.length; i++) {
                    if (torrents[i].hash == data.hash) {
                        torrents.splice(i, 1);
                        break;
                    }
                }
                that.update();
            });
            obs.on('addTorrent', function (data) {
                console.log('addTorrent')
                console.log(data)

                that.opts.torrents.push(data);
                that.update();
            });
        });
    </script>
</torrents>