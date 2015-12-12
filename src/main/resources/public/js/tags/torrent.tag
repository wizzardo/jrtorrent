<torrent id="torrent_{hash}">
    <div class="torrent {status.toLowerCase()} {opts.header?'header':''}">
        <span class="name">{name || opts.name}</span>
        <span class="status">{status || opts.status}</span>
        <span class="size">{size || opts.size}</span>
        <span class="d">{d || opts.d}</span>
        <span class="u">{u  || opts.u}</span>
        <span class="ds">{ds  || opts.ds || 0}</span>
        <span class="us">{us || opts.us || 0}</span>
        <span class="peers">{p || opts.p} {pt ? '('+pt+')':''}</span>
        <span class="seeds">{s || opts.s} {st ? '('+st+')':''}</span>

        <div class="mdl-progress">
            <div class="progressbar bar bar1" style="width: {progress}%;"></div>
            <div class="bufferbar bar bar2"></div>
        </div>
    </div>

    <style scoped>
        :scope {
            display: block;
            margin: 10px;
        }
        .torrent:hover{
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
            width: 50px;
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
        })
    </script>
</torrent>
