<torrents>
    <div class="header">
        <span class="th status">STATUS</span>
        <span class="th size">SIZE</span>
        <span class="th d">DL</span>
        <span class="th ds">↓SPEED</span>
        <span class="th eta">ETA</span>
        <span class="th u">UL</span>
        <span class="th us">↑SPEED</span>
        <span class="th peers">PEERS</span>
        <span class="th seeds">SEEDS</span>
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

        /*.header */
        .th {
            width: 80px;
            display: inline-block;
        }

        .th.status {
            width: 120px;
        }

        @media screen and (max-width: 850px) {
            .header {
                padding-left: 11px;
                margin: 0;
            }

            .status, .peers, .seeds, .size {
                display: none;
            }
        }

        @media screen and (max-width: 480px) {
            .th {
                width: 64px;
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