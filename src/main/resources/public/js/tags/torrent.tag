<torrent class="torrent {status.toLowerCase()}" id="torrent_{hash}">
    <span class="name">{name}</span>

    <div class="mdl-progress">
        <div class="progressbar bar bar1" style="width: {progress}%;"></div>
        <div class="bufferbar bar bar2"></div>
    </div>

    <style scoped>
        :scope {
            display: block
        }

        .mdl-progress {
            width: 200px;
        }

        .mdl-progress > .bufferbar {
            width: 100%;
        }
    </style>

    <script>
        var that = this;

        this.on('mount', function () {
            that.obs.on('update_progress_' + that.hash, function (progress, callback) {
//                console.log('update_progress: '+progress);
                that.progress = progress;
                that.update()
            });
        })
    </script>
</torrent>
