<tree>
    <div class="resizeable" style="height: {30 * totalChildrenToShow || 0}px">
        <tree_entry each={opts.entries}></tree_entry>
    </div>
    <style scoped>
        :scope {
            display: block;
            overflow: hidden;
            background: #FAFAFA;
            padding-left: 10px;
            border-bottom: 1px solid #EAEAEA;
        }

        .resizeable {
            transition: height .2s cubic-bezier(.4, 0, .2, 1);
        }
    </style>

    <script>
        var that = this;
        var opened = false;
        var hash = opts.hash;
        var totalChildrenToShow = 0;
        var totalChildrenHidden = 0;

        this.on('mount', function () {
            that.totalChildrenHidden = opts.entries.length;
            that.update();
        });

        that.updateShownChildsCount = function (innerChilds) {
//            log('tree.updateShownChildsCount ' + innerChilds)
            if (typeof(innerChilds) != "undefined")
                that.totalChildrenToShow = that.totalChildrenToShow + innerChilds;
            log('tree.totalChildrenToShow ' + that.totalChildrenToShow)
        };

        that.toggle = function () {
            that.openned = !that.openned;
            log('toggle tree: ' + that.openned);
            if (!that.openned) {
                that.totalChildrenHidden = that.totalChildrenToShow;
                that.totalChildrenToShow = 0;
            } else {
                that.totalChildrenToShow = that.totalChildrenHidden;
            }
            that.update();
        };

        that.setPriorityForEntry = function (entry, value) {
            for (var i = 0; i < opts.entries.length; i++) {
                var it = opts.entries[i];
                if (it.name == entry)
                    it.priority = value;
            }
        };

        that.path = function () {
            return config.downloadsPath + (that.opts.entries.length != 1 ? '/' + encodeURIComponent(that.opts.name) : '')
        };

    </script>
</tree>