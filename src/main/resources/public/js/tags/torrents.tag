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
            margin: 20px 10px 0 10px;
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

        @media screen and (max-width: 850px) {
        }

        @media screen and (max-width: 800px) {
            torrents .status {
                display: none;
            }
        }

        @media screen and (max-width: 736px) {
            torrents .header {
                padding-left: 11px;
                margin: 0;
            }

            torrents .th {
                width: 86px;
            }
        }

        @media screen and (max-width: 690px) {
            torrents .th {
                width: 81px;
            }
        }

        @media screen and (max-width: 667px) {
            torrents .th {
                width: 78px;
            }
        }

        @media screen and (max-width: 600px) {
            torrents .th {
                width: 69px;
            }
        }

        @media screen and (max-width: 568px) {
            torrents .th {
                width: 66px;
            }
        }

        @media screen and (max-width: 480px) {
            torrents .th {
                width: 64px;
            }

            torrents .peers, torrents .seeds, torrents .size {
                display: none;
            }
        }

        @media screen and (max-width: 414px) {
            torrents .th {
                width: 70px;
            }
        }

        @media screen and (max-width: 412px) {
            torrents .th {
                width: 75px;
            }
        }

        @media screen and (max-width: 384px) {
            torrents .th {
                width: 70px;
            }
        }

        @media screen and (max-width: 360px) {
            torrents .th {
                width: 68px;
            }
        }

        @media screen and (max-width: 320px) {
            torrents .th {
                width: 54px;
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