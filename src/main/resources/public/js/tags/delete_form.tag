<delete_form>
    Delete torrent?
    <p> Are you sure that you want to delete '{name || 'name'}'</p>

    <form action="#" name="form" onsubmit={ submit }>
        <button class="mdl-button mdl-js-button mdl-button--raised mdl-button--colored"
                onclick="{deleteTorrentWithData}">
            Delete with data
        </button>
        <button class="mdl-button mdl-js-button mdl-button--raised mdl-button--colored" onclick="{deleteTorrent}">
            Delete
        </button>
        <button class="mdl-button mdl-js-button mdl-button--raised" onclick="{cancel}">
            Cancel
        </button>
    </form>

    <style scoped>
        :scope {
            display: block;
            position: relative;
            height: 100%;
        }

        form {
            position: absolute;
            right: 0px;
            bottom: 0px;
            margin: 0px;
        }

        button {
            margin-left: 20px;
        }

        @media screen and (max-width: 480px) {
            button {
                margin-left: 0px;
                margin-bottom: 5px;
                width: 100%;
                display: block;
            }
        }
    </style>

    <script>
        var that = this;
        this.on('mount', function () {
        });

        that.cancel = function () {
            return true;
        };

        that.deleteTorrent = function () {
            obs.trigger('sendDeleteTorrent');
            return true;
        };

        that.deleteTorrentWithData = function () {
            obs.trigger('sendDeleteTorrent');
            return true;
        };

        that.submit = function (e) {
            return false
        }
    </script>
</delete_form>