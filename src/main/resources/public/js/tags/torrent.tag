<torrent id="torrent_{hash}">
    <div class="status-bar {status}"></div>
    <div class="torrent {status.toLowerCase()}" onclick="toggleTree(event, '{hash}')">
        <button class="mdl-button mdl-js-button mdl-button--icon pause" onclick="pauseTorrent('{hash}')">
            <i class="material-icons">{status =='PAUSED' || status == 'STOPPED' ? 'play_arrow' : 'pause'}</i>
        </button>
        <button class="mdl-button mdl-js-button mdl-button--icon delete-left"
                onclick="deleteTorrent('{hash}','{name}')">
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
        <button class="mdl-button mdl-js-button mdl-button--icon delete" onclick="deleteTorrent('{hash}','{name}')">
            <i class="material-icons">delete</i>
        </button>
    </div>
    <tree id="tree_{hash}"></tree>

    <style scoped>
        :scope {
            display: block;
            position: relative;
        }

        .mdl-button {
            margin-right: 10px;
        }

        .material-icons {
            color: #757575;
        }

        .status-bar {
            width: 4px;
            height: 100%;
            position: absolute;
            z-index: 2;
        }

        .status-bar.FINISHED {
            background-color: rgb(63, 81, 181);
        }

        .status-bar.PAUSED {
            background-color: rgb(255, 193, 7);
        }

        .status-bar.DOWNLOADING {
            background-color: rgb(76, 175, 80);
        }

        .status-bar.SEEDING {
            background-color: rgb(255, 87, 34);
        }

        .torrent {
            padding: 10px;
            min-height: 40px;
            position: relative;
            left: 0px;
            transition: left .2s cubic-bezier(.4, 0, .2, 1);
        }

        .torrent > div {
            display: inline-block;
        }

        .torrent:hover {
            background-color: ghostwhite;
        }

        .torrent .name {
            font-weight: bold;
            display: block;
            width: 100%;
        }

        .torrent .status {
            width: 120px;
            display: inline-block;
        }

        span {
            text-overflow: ellipsis;
            white-space: nowrap;
            overflow: hidden;
        }

        .td {
            width: 80px;
            display: inline-block;
        }

        .mdl-progress {
            width: 100%;
        }

        .mdl-progress > .bufferbar {
            width: 100%;
        }

        .header {
            border-bottom: 1px solid gray;
        }

        .header span {
            font-weight: bold;
        }

        .header .mdl-progress {
            display: none;
        }

        @media screen and (max-width: 800px) {
            .torrent .status {
                display: none;
            }
        }

        @media screen and (max-width: 768px) {
            .mdl-progress {
                width: 100%;
            }
        }

        @media screen and (min-width: 737px) {
            .delete-left {
                display: none;
            }
        }

        @media screen and (max-width: 736px) {
            .torrent {
                width: 850px;
                left: -90px;
            }

            .td {
                width: 86px;
            }

            .torrent:hover {
                left: 0px;
            }

            .delete {
                display: none;
            }
        }

        @media screen and (max-width: 690px) {
            .td {
                width: 81px;
            }
        }

        @media screen and (max-width: 667px) {
            .td {
                width: 78px;
            }
        }

        @media screen and (max-width: 600px) {
            .td {
                width: 69px;
            }
        }

        @media screen and (max-width: 568px) {
            .td {
                width: 66px;
            }
        }

        @media screen and (max-width: 480px) {
            .td {
                width: 70px;
            }

            .peers, .seeds, .size {
                display: none;
            }

            .torrent {
                min-height: 55px;
            }

            .header {
                display: none;
            }
        }

        @media screen and (max-width: 412px) {
            .td {
                width: 75px;
            }
        }

        @media screen and (max-width: 384px) {
            .td {
                width: 70px;
            }
        }

        @media screen and (max-width: 375px) {
            .torrent .mdl-progress, .name {
                max-width: 355px;
            }

            .td {
                width: 68px;
            }
        }

        @media screen and (max-width: 360px) {
            .torrent .mdl-progress, .name {
                max-width: 340px;
            }
        }

        @media screen and (max-width: 320px) {
            .td {
                width: 55px;
                font-size: 12px;
            }

            .torrent .mdl-progress, .name {
                max-width: 300px;
            }
        }
    </style>

    <script>
        var that = this;

        this.on('mount', function () {
            console.log('on mount torrent: ' + that.hash);
            console.log(that);

            obs.on('update_' + that.hash, function (data) {
                console.log('on update torrent: ' + that.hash);
                console.log(data);
//                console.log('update_progress: '+progress);
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
            obs.on('toggle_tree_' + that.hash, function () {
                console.log('on toggle_tree_' + that.hash + (that.showTree ? ' close' : ' open'));
                that.showTree = !that.showTree;

                if (!that.tree)
                    obs.trigger('load_tree', {hash: that.hash});
                else
                    that.tree.toggle();

//                that.update()
            });
            obs.on('tree_loaded_' + that.hash, function (data) {
                console.log('tree_loaded ' + data);
                console.log(data);
                that.tree = riot.mount('#tree_' + that.hash, {entries: data, hash: that.hash, name: that.name})[0];
                setTimeout(that.tree.toggle, 1);
//                that.tree.toggle();
            });
            obs.on('torrent_toggle_' + that.hash, function () {
                if (that.status == 'STOPPED' || that.status == 'PAUSED')
                    obs.trigger('torrent.start', {hash: that.hash});
                else
                    obs.trigger('torrent.stop', {hash: that.hash});
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
            if (isNaN(s))
                return '';
            if (s < 60)
                return Math.ceil(s) + 's';
            var m = s / 60;
            if (m < 60)
                return Math.ceil(m) + 'm' + Math.ceil(s % 60) + 's';
            var h = m / 60;
            if (h < 24)
                return Math.ceil(h) + 'h' + Math.ceil(m % 60) + 'm';
            var d = h / 24;
            return Math.ceil(d) + 'd' + Math.ceil(h % 24) + 'h';
        };

        toggleTree = function (event, hash) {
            if (event.processed)
                return true;
            console.log('toggle_tree_' + hash);
            obs.trigger('toggle_tree_' + hash);
        };

        pauseTorrent = function (hash) {
            console.log('pause ' + hash);
            obs.trigger('torrent_toggle_' + hash);
            event.processed = true;
            return true;
        };

        deleteTorrent = function (hash, name) {
            console.log('deleteTorrent ' + hash);
            obs.trigger('torrent.delete', {hash: hash, name: name});
            event.processed = true;
            return true;
        };
    </script>
</torrent>
