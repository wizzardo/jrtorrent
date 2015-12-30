<add_button>
    <button class="mdl-button mdl-js-button" onclick="{show}">
        Add
    </button>

    <style scoped>
        :scope {
            display: inline;
        }

        .mdl-button {
            font-weight: bold;
            color: white;
        }
    </style>

    <script>
        var that = this;

        this.on('mount', function () {
            that.modal = riot.mount('#add_modal', {})[0];

            obs.on('sendAddTorrent', function () {
                that.modal.close()
            })
        });

        that.show = function () {
            that.modal.toggle();
        }
    </script>
</add_button>