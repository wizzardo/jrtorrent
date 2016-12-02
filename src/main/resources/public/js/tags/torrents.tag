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

    <style>
        torrents {
            display: block
        }

        torrents .header {
            margin: 0px 10px 0 10px;
            border-bottom: 1px solid gray;
            padding-left: 44px;
        }

        torrents .header span {
            font-weight: bold;
        }

        /*.header */
        torrents .th {
            width: 80px;
            display: inline-block;
        }

        torrents .th.status {
            width: 120px;
        }

        @media screen and (max-width: 800px) {
            torrents .status, torrents .size {
                display: none;
            }
        }

        @media screen and (max-width: 736px) {
            torrents .header {
                padding-left: 5px;
                margin: 0;
            }
        }

        @media screen and (max-width: 480px) {
            torrents .th {
                width: 20vw;
            }

            torrents .peers, torrents .seeds, torrents .size {
                display: none;
            }
        }
    </style>
    <script>
        var that = this;

        that.on('mount', function () {
            obs.set('removeTorrent', function (data) {
                var torrents = that.opts.torrents;
                for (var i = 0; i < torrents.length; i++) {
                    if (torrents[i].hash == data.hash) {
                        torrents.splice(i, 1);
                        break;
                    }
                }
                that.update();
            });
            obs.set('addTorrent', function (data) {
                log('addTorrent')
                log(data)

                that.opts.torrents.splice(0, 0, data);
                that.update();
            });
        });
    </script>
</torrents>