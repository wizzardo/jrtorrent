<torrent class="torrent {status.toLowerCase()}" id="torrent_{hash}">
    <span class="name">{name}</span>
    <span class="progress">{progress}%</span>

    <style scoped>
        :scope {
            display: block
        }
    </style>

    <script>
        var that = this;

        this.on('mount', function () {
            that.obs.on('update_progress_' + that.hash, function (progress, callback) {
//                console.log('update_progress: '+progress);
                that.progress = progress
                that.update()
            });
        })
    </script>
</torrent>
