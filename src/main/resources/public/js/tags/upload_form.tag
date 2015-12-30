<upload_form>
    Add new torrent
    <form action="#" name="form" onsubmit={ submit }>
        <div class="mdl-textfield mdl-js-textfield mdl-textfield--floating-label">
            <input class="mdl-textfield__input" type="text" id="url" name="url">
            <label class="mdl-textfield__label" for="url">link to torrent file or magnet link</label>
        </div>

        <br/>

        <div class="mdl-textfield mdl-js-textfield mdl-textfield--file">
            <input class="mdl-textfield__input" placeholder="File" type="text" name="fileName" readonly/>

            <div class="mdl-button mdl-button--primary mdl-button--icon mdl-button--file">
                <i class="material-icons">attach_file</i>
                <input type="file" name="file">
            </div>
        </div>

        <br/>

        <button class="mdl-button mdl-js-button mdl-button--raised mdl-button--colored download" onclick="{sendForm}">
            Download
        </button>
    </form>

    <style>
        :scope {
            display: block;
        }

        form {
            position: relative;
            width: 100%;
            height: 90%;
        }

        .download {
            position: absolute;
            right: 0px;
            bottom: 0px;
        }

        .mdl-button--file input {
            cursor: pointer;
            height: 100%;
            right: 0;
            opacity: 0;
            position: absolute;
            top: 0;
            width: 300px;
            z-index: 4;
        }

        .mdl-textfield--file .mdl-textfield__input {
            box-sizing: border-box;
            width: calc(100% - 32px);
        }

        .mdl-textfield--file .mdl-button--file {
            right: 0;
        }

        .mdl-textfield {
            width: 100%;
        }

    </style>

    <script>
        var that = this;
        this.on('mount', function () {
            that.file.onchange = function () {
                that.fileName.value = this.files[0].name;
            };
        });

        that.sendForm = function () {
            lib.ajax({
                data: new FormData(that.form),
                url: '/addTorrent',
                method: 'POST',
                success: function () {
                    that.url.value = '';
                    that.file.value = '';
                }

            });
            obs.trigger('sendAddTorrent');
            return true;
        };

        that.submit = function (e) {
            return false
        }

    </script>
</upload_form>