<tree>
    <div class="resizeable" style="height: {25 * totalChildrenToShow}px">
        <tree_entry each={opts.entries}></tree_entry>
    </div>
    <style scoped>
        :scope {
            display: block;
            overflow: hidden;
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

        this.on('mount', function () {
//            that.updateShownChildsCount()
            that.totalChildrenToShow = opts.entries.length;
        });

        that.updateShownChildsCount = function (innerChilds) {
//            console.log('tree.updateShownChildsCount ' + innerChilds)
            if (typeof(innerChilds) != "undefined")
                that.totalChildrenToShow = that.totalChildrenToShow + innerChilds;
            console.log('tree.totalChildrenToShow ' + that.totalChildrenToShow)
        };

    </script>
</tree>