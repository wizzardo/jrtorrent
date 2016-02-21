<mdl_select>

    <ul class="mdl-menu mdl-menu--bottom-right mdl-js-menu mdl-js-ripple-effect" for="{opts.for}">
        <li each="{ item, i in opts.items }" class="mdl-menu__item" data-n="{i}" onclick="{click}">
            { item }
        </li>
    </ul>

    <style scoped>

    </style>

    <script>
        var that = this;
        this.mixin(mdlMixin);
        var menu;
        var callback = function (e) {
            if (e.target.parentNode == document.getElementById(that.opts.for))
                return;

            menu.MaterialMenu.hide();
            setTimeout(function () {
                that.unmount(true);
            }, 1000);
        };

        this.on('mount', function () {
            menu = that.root.querySelector('.mdl-menu');
            menu.MaterialMenu.show();

            setTimeout(function () {
                document.addEventListener('click', callback);
            }, 50)
        });
        this.on('unmount', function () {
            document.removeEventListener('click', callback);
            if (that.opts.onUnMount)
                that.opts.onUnMount()
        });

        that.click = function (i) {
            log(i);
            if (that.opts.onSelect)
                that.opts.onSelect(+i.target.dataset.n)
        };

    </script>
</mdl_select>