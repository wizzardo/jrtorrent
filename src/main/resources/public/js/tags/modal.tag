<modal>

    <div name="overlay" class="overlay" onclick="{close}"
         style="top:{display?0:-100}%;background-color: rgba(0, 0, 0, {show?0.5:0})">
        <div class="content" style="margin-top: {show?20:-100}%;">
            <yield/>
        </div>
    </div>

    <style scoped>
        :scope {
            display: block;
        }

        .overlay {
            position: fixed;
            width: 100%;
            height: 100%;
            z-index: 100;
            top: 0;
            left: 0;
            transition: background-color .2s cubic-bezier(.4, 0, .2, 1);
        }

        .content {
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
            .content {
                max-width: 600px;
            }
        }
    </style>

    <script>
        var that = this;
        this.display = false;
        this.mixin(mdlMixin);

        this.on('mount', function () {
            console.log('modal mounted')
            that.overlay.addEventListener("transitionend", function () {
                if (!that.show) {
                    that.display = false;
                    that.update();
                }
            }, false);
            if (that.opts.onMount)
                that.opts.onMount(that);
        });

        that.close = function (e) {
            if (e && e.target != that.overlay)
                return true;

            that.show = false;
            that.update();
        };
        that.open = function () {
            that.display = true;
            that.show = true;
            that.update();
        };
        that.toggle = function () {
            that.show = !that.show;
            if (that.show)
                that.display = true;

            that.update();
        }
    </script>

</modal>