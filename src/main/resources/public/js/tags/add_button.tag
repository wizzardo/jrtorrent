<add_button>
    <button class="mdl-button mdl-js-button mdl-js-button mdl-button--fab mdl-js-ripple-effect
                   mdl-button--colored mdl-button--floating-action {hidden?'slide-bottom':''}" onclick="{show}">
        <i class="material-icons">add</i>
    </button>

    <style>
        add_button {
            display: inline;
        }

        add_button .mdl-button {
            font-weight: bold;
            color: white;
        }

        add_button .mdl-button--floating-action {
            position: fixed;
            right: 24px;
            bottom: 24px;
            padding-top: 24px;
            margin-bottom: 0;
            z-index: 998;
            transition: bottom .2s cubic-bezier(.4, 0, .2, 1);
        }

        add_button .slide-bottom {
            bottom: -100px;
        }

        add_button .mdl-button--fab {
            border-radius: 77% 23% 37% 63% / 34% 38% 62% 66%;
        }

        add_button .mdl-button--fab .mdl-button__ripple-container {
            border-radius: 77% 23% 37% 63% / 34% 38% 62% 66%;
        }
    </style>

    <script>
        var that = this;

        this.on('mount', function () {
            that.modal = riot.mount('#add_modal', {})[0];
            that.modal.onShow(function () {
                that.hidden = true;
                that.update();
            });
            that.modal.onHide(function () {
                that.hidden = false;
                that.update();
            });

            obs.on('sendAddTorrent', function () {
                that.modal.close()
            })
        });

        that.show = function () {
            that.modal.toggle();
        }
    </script>
</add_button>