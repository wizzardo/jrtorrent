<modal>

    <div name="overlay" class="overlay" onclick="{close}"
         style="top:{display?0:-100}%;background-color: rgba(0, 0, 0, {show?0.5:0})">
        <div class="content" style="margin-top: {show?20:-100}%;">
            <yield/>
        </div>
    </div>

    <style>
        modal {
            display: block;
        }

        modal .overlay {
            position: fixed;
            width: 100%;
            height: 100%;
            z-index: 100;
            top: 0;
            left: 0;
            transition: background-color .2s cubic-bezier(.4, 0, .2, 1);
        }

        modal .content {
            height: 200px;
            background-color: white;
            margin-left: auto;
            margin-right: auto;
            font-size: 16px;
            max-width: 80%;
            margin-top: 20%;
            padding: 20px;
            box-shadow: 1px 1px 4px 0px rgba(0, 0, 0, 0.75);
            transition: margin-top .2s cubic-bezier(.4, 0, .2, 1);
        }

        @media screen and (min-width: 800px) {
            modal .content {
                max-width: 600px;
            }
        }
        @media screen and (max-width: 800px) {
            modal .overlay {
                transition: none;
            }
            modal .content {
                box-shadow: none;
            }
        }
    </style>

    <script>
        var that = this;
        this.display = false;
        this.mixin(mdlMixin);

        this.on('mount', function () {
            log('modal mounted')
            that.overlay.addEventListener("transitionend", function () {
                if (!that.show) {
                    that.display = false;
                    that.update();
                    that.trigger('hide')
                } else {
                    that.trigger('show')
                }
            }, false);
        });
        obs.on('modal.closeAll', function () {
            that.close();
        });

        that.close = function (e) {
            if (e && e.target != that.overlay)
                return true;

            that.show = false;
            that.update();
        };
        that.open = function () {
            obs.trigger('modal.closeAll');
            that.display = true;
            that.show = true;
            that.update();
        };
        that.toggle = function () {
            if (that.show)
                that.close();
            else
                that.open();
        };
        that.onShow = function (cb) {
            that.on('show', cb);
        };
        that.onHide = function (cb) {
            that.on('hide', cb);
        };
    </script>

</modal>