<disk_status>
    <div>
        <span class="info">Free disk space:</span>
        <span class="value">{formatNumber(free || 0)}</span>
        <span class="info">{formatAbbreviation(free || 0)}</span>
    </div>

    <style>
        disk_status div {
            display: inline-block;
            margin-left: 50px;
            margin-top: 10px;
            margin-bottom: 10px;
        }
        disk_status .value {
            font-weight: bold;
            font-size: 2em;
        }
        disk_status .info {
            padding-right: 10px;
            padding-left: 10px;
        }
    </style>

    <script>
        var that = this;

        this.on('mount', function () {
            obs.set('updateDiskStatus', function (data) {
                log('updateDiskStatus ' + data);
                log(data);

                that.free = data.free;
                that.update()
            });
        });

        formatNumber = function (size) {
            if (size < 1024)
                return size;
            size /= 1024;
            if (size < 1024)
                return size.toFixed(3);
            size /= 1024;
            if (size < 1024)
                return size.toFixed(3);
            size /= 1024;
            return size.toFixed(3);
        };
        formatAbbreviation = function (size) {
            if (size < 1024)
                return ' B';
            size /= 1024;
            if (size < 1024)
                return ' KB';
            size /= 1024;
            if (size < 1024)
                return ' MB';
            size /= 1024;
            return ' GB';
        };
    </script>
</disk_status>