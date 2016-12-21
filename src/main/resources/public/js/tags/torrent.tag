<torrent>
    <div class="status-bar {status}"></div>
    <div class="torrent {status.toLowerCase()} {selected ? 'selected' : ''}" onclick="{clickTorrent}">
        <button class="mdl-button mdl-js-button mdl-button--icon pause" onclick="{pauseTorrent}">
            <i class="material-icons">{status =='PAUSED' || status == 'STOPPED' || status == 'FINISHED' ? 'play_arrow' : 'pause'}</i>
        </button>
        <button class="mdl-button mdl-js-button mdl-button--icon delete-left"
                onclick="{deleteTorrent}">
            <i class="material-icons">delete</i>
        </button>
        <div>
            <span class="td name">{name || opts.name}</span>
            <span class="td status">{status || opts.status}</span>
            <span class="td size">{formatSize(size || opts.size)}</span>
            <span class="td d">↓{formatSize(d || opts.d || 0)}</span>
            <span class="td ds">↓{formatSpeed(ds || opts.ds || 0)}</span>
            <span class="td eta">{progress == 100 || size - d == 0 ? '' : formatEta(size - d, ds)}</span>
            <span class="td u">↑{formatSize(u || opts.u || 0)}</span>
            <span class="td us">↑{formatSpeed(us || opts.us || 0)}</span>
            <span class="td peers">{p || opts.p} {pt ? '('+pt+')':''}</span>
            <span class="td seeds">{s || opts.s} {st ? '('+st+')':''}</span>

            <div class="mdl-progress">
                <div class="progressbar bar bar1" style="width: {progress}%;"></div>
                <div class="bufferbar bar bar2"></div>
            </div>
        </div>
        <button class="mdl-button mdl-js-button mdl-button--icon delete" onclick="{deleteTorrent}">
            <i class="material-icons">delete</i>
        </button>
    </div>
    <tree id="tree_{hash}"></tree>

    <style>
        torrent {
            display: block;
            position: relative;
        }

        torrent .mdl-button {
            margin-right: 10px;
        }

        torrent .material-icons {
            color: #757575;
        }

        torrent .status-bar {
            width: 4px;
            height: 100%;
            position: absolute;
            z-index: 2;
        }

        torrent .status-bar.FINISHED {
            background-color: rgb(63, 81, 181);
        }

        torrent .status-bar.STOPPED {
            background-color: rgb(255, 193, 7);
        }

        torrent .status-bar.DOWNLOADING {
            background-color: rgb(76, 175, 80);
        }

        torrent .status-bar.SEEDING {
            background-color: rgb(255, 87, 34);
        }

        torrent .torrent {
            padding: 10px;
            min-height: 40px;
            position: relative;
            left: 0px;
            transition: left .2s cubic-bezier(.4, 0, .2, 1);
        }

        torrent .torrent:hover {
            cursor: pointer;
        }

        torrent .torrent > div {
            display: inline-block;
        }

        torrent .torrent.selected {
            background: #FAFAFA;
        }

        torrent .torrent:hover {
            background-color: rgba(0, 0, 0, 0.05);
        }

        torrent .torrent .name {
            font-weight: bold;
            display: block;
            width: 100%;
            max-width: 760px;
        }

        torrent .torrent .status {
            width: 120px;
            display: inline-block;
        }

        torrent span {
            text-overflow: ellipsis;
            white-space: nowrap;
            overflow: hidden;
        }

        torrent .td {
            width: 80px;
            display: inline-block;
        }

        torrent .mdl-progress {
            width: 100%;
        }

        torrent .mdl-progress > .bufferbar {
            width: 100%;
        }

        @media screen and (max-width: 800px) {
            torrent .torrent .status {
                display: none;
            }
        }

        @media screen and (min-width: 737px) {
            torrent .delete-left {
                display: none;
            }
        }

        @media screen and (max-width: 736px) {
            torrent .torrent {
                left: -90px;
            }

            torrent .torrent .name {
                padding-left: 5px;
            }

            torrent .torrent.selected {
                left: 0px;
            }

            torrent .delete {
                display: none;
            }

            torrent .mdl-progress {
                width: 100vw;
            }
        }

        @media screen and (max-width: 480px) {
            torrent .td {
                width: 20vw;
            }

            torrent .peers, torrent .seeds, torrent .size {
                display: none;
            }

            torrent .torrent {
                min-height: 55px;
            }
        }

        @media screen and (max-width: 320px) {
            torrent .td {
                font-size: 12px;
            }
        }
    </style>

    <script>
        var that = this;
        that.selected = false;

        this.on('mount', function () {
            log('on mount torrent: ' + that.hash);
            log(that);

            obs.set('update_' + that.hash, function (data) {
                log('on update torrent: ' + that.hash);
                log(data);
//                log('update_progress: '+progress);
                if (that.name != data.name)
                    that.name = data.name;
                if (that.progress != data.progress)
                    that.progress = data.progress;
                if (that.status != data.status)
                    that.status = data.status;
                if (that.size != data.size)
                    that.size = data.size;
                if (that.d != data.d)
                    that.d = data.d;
                if (that.u != data.u)
                    that.u = data.u;
                if (that.ds != data.ds)
                    that.ds = data.ds;
                if (that.us != data.us)
                    that.us = data.us;
                if (that.p != data.p)
                    that.p = data.p;
                if (that.pt != data.pt)
                    that.pt = data.pt;
                if (that.s != data.s)
                    that.s = data.s;
                if (that.st != data.st)
                    that.st = data.st;
                that.update()
            });
            that.toggleTree = function () {
                log('on toggle_tree_' + that.hash + (that.showTree ? ' close' : ' open'));
                that.showTree = !that.showTree;

                if (!that.tree)
                    obs.trigger('load_tree', {hash: that.hash});
                else
                    that.tree.toggle();

//                that.update()
            };
            obs.set('toggle_tree_' + that.hash, that.toggleTree);
            obs.set('tree_loaded_' + that.hash, function (data) {
                log('tree_loaded ' + data);
                log(data);
                that.tree = riot.mount('#tree_' + that.hash, {entries: data, hash: that.hash, name: that.name})[0];
                setTimeout(that.tree.toggle, 1);
//                that.tree.toggle();
            });
            obs.set('torrent_toggle_' + that.hash, function () {
                if (that.status == 'STOPPED' || that.status == 'PAUSED' || that.status == 'FINISHED')
                    obs.trigger('torrent.start', {hash: that.hash});
                else
                    obs.trigger('torrent.stop', {hash: that.hash});
            });
            obs.set('click_' + that.hash, function () {
                log('toggle_tree_' + that.hash);
                that.toggleTree();
                that.selected = !that.selected;
                that.update();
            });
        });

        formatSize = function (size) {
            if (size < 1024)
                return size + 'B';
            size /= 1024;
            if (size < 1024)
                return size.toFixed(1) + 'KB';
            size /= 1024;
            if (size < 1024)
                return size.toFixed(1) + 'MB';
            size /= 1024;
            return size.toFixed(1) + 'GB';
        };
        formatSpeed = function (size) {
            return formatSize(size) + '/s'
        };
        formatEta = function (remaining, speed) {
            var s = remaining / speed;
            if (!isFinite(s))
                return '';
            if (s < 60)
                return Math.floor(s) + 's';
            var m = s / 60;
            if (m < 60)
                return Math.floor(m) + 'm' + Math.floor(s % 60) + 's';
            var h = m / 60;
            if (h < 24)
                return Math.floor(h) + 'h' + Math.floor(m % 60) + 'm';
            var d = h / 24;
            return Math.floor(d) + 'd' + Math.floor(h % 24) + 'h';
        };

        that.clickTorrent = function (event) {
            if (event.processed)
                return true;

            obs.trigger('click_' + that.hash);
        };

        that.pauseTorrent = function () {
            log('pause ' + that.hash);
            obs.trigger('torrent_toggle_' + that.hash);
            event.processed = true;
            return true;
        };

        that.deleteTorrent = function () {
            log('deleteTorrent ' + that.hash);
            obs.trigger('torrent.delete', {hash: that.hash, name: that.name});
            event.processed = true;
            return true;
        };
    </script>
</torrent>
