<tree>
    <div class="resizeable" style="height: {30 * totalChildrenToShow || 0}px">
        <tree_entry each={opts.entries}></tree_entry>
    </div>
    <style>
        tree {
            display: block;
            background: #FAFAFA;
            padding-left: 10px;
            border-bottom: 1px solid #EAEAEA;
            overflow: hidden;
        }

        @media screen and (max-width: 960px) {
            tree {
                overflow-x: auto;
                max-width: 100vw;
            }
        }

        tree .resizeable {
            transition: height .2s cubic-bezier(.4, 0, .2, 1);
        }
    </style>

    <script>
        var that = this;
        var opened = false;
        var totalChildrenToShow = 0;
        var totalChildrenHidden = 0;

        this.on('mount', function () {
            that.totalChildrenHidden = (opts.entries || []).length;
            that.update();
        });

        that.updateShownChildsCount = function (innerChilds) {
//            log('tree.updateShownChildsCount ' + innerChilds)
            if (typeof (innerChilds) != "undefined")
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
            return ''
        };

        that.hash = function () {
            return that.opts.hash;
        };

    </script>
</tree>