<torrent id="torrent_{hash}">
    <div class="torrent {status.toLowerCase()}" onclick="toggleTree('{hash}')">
        <button class="mdl-button mdl-js-button mdl-button--icon pause">
            <i class="material-icons">pause</i>
        </button>
        <button class="mdl-button mdl-js-button mdl-button--icon delete-left">
            <i class="material-icons">delete</i>
        </button>
        <div>
            <span class="name">{name || opts.name}</span>
            <span class="status">{status || opts.status}</span>
            <span class="size">{formatSize(size || opts.size)}</span>
            <span class="d">↓{formatSize(d || opts.d || 0)}</span>
            <span class="ds">↓{formatSpeed(ds || opts.ds || 0)}</span>
            <span class="u">↑{formatSize(u || opts.u || 0)}</span>
            <span class="us">↑{formatSpeed(us || opts.us || 0)}</span>
            <span class="peers">{p || opts.p} {pt ? '('+pt+')':''}</span>
            <span class="seeds">{s || opts.s} {st ? '('+st+')':''}</span>

            <div class="mdl-progress">
                <div class="progressbar bar bar1" style="width: {progress}%;"></div>
                <div class="bufferbar bar bar2"></div>
            </div>
        </div>
        <button class="mdl-button mdl-js-button mdl-button--icon delete">
            <i class="material-icons">delete</i>
        </button>
    </div>
    <tree id="tree_{hash}" show="{showTree}"></tree>

    <style scoped>
        :scope {
            display: block;
        }

        .mdl-button {
            margin-right: 10px;
        }

        .material-icons {
            color: #757575;
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

        .name {
            font-weight: bold;
            width: 150px;
            display: inline-block;
        }

        .status {
            width: 120px;
            display: inline-block;
        }

        span {
            text-overflow: ellipsis;
            white-space: nowrap;
            overflow: hidden;
        }

        .size, .d, .ds, .u, .us, .peers, .seeds {
            width: 63px;
            display: inline-block;
        }

        .mdl-progress {
            width: 200px;
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

        @media screen and (max-width: 850px) {
            .mdl-button {
                /*display: none;*/
            }

            .torrent {
                min-height: 55px;
            }

            .header {
                display: none;
            }

            .mdl-progress {
                width: 100%;
            }

            .status, .peers, .seeds {
                display: none;
            }

            .name {
                width: 100%;
            }
        }

        @media screen and (min-width: 481px) {
            .delete-left {
                display: none;
            }
        }

        @media screen and (max-width: 480px) {
            .delete {
                display: none;
            }

            .torrent {
                width: 700px;
                left: -90px;
            }

            .torrent:hover {
                left: 0px;
            }
        }

        @media screen and (max-width: 375px) {

            .torrent .mdl-progress {
                max-width: 355px;
            }
        }

        @media screen and (max-width: 360px) {
            .torrent .mdl-progress {
                max-width: 340px;
            }
        }

        @media screen and (max-width: 320px) {
            .size, .d, .ds, .u, .us, .peers, .seeds {
                width: 55px;
                font-size: 12px;
            }

            .torrent .mdl-progress {
                max-width: 300px;
            }
        }
    </style>

    <script>
        var that = this;

        this.on('mount', function () {
            that.obs.on('update_' + that.hash, function (data) {
//                console.log('update_progress: '+progress);
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
            that.obs.on('toggle_tree_' + that.hash, function () {
                console.log('on toggle_tree_' + that.hash + (that.showTree ? ' close' : ' open'));
                that.showTree = !that.showTree;

                if (that.showTree)
                    that.obs.trigger('load_tree', {hash: that.hash});
                that.update()
            });
            that.obs.on('tree_loaded_' + that.hash, function (data) {
                console.log('tree_loaded ' + data);
                console.log(data);
                riot.mount('#tree_' + that.hash, {entries: data, hash: that.hash})
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

        toggleTree = function (hash) {
            console.log('toggle_tree_' + hash);
            that.obs.trigger('toggle_tree_' + hash);
        }
    </script>
</torrent>
