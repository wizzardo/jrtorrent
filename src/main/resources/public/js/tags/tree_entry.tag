<tree_entry>
    <div onclick="{toggleChildren}">
        <i if="{isFolder}" class="material-icons">{showChildren?'folder_open':'folder'}</i>
        <span>{isFolder?name:''}</span>
        <a if="{!isFolder}" href="{path() + '?token=' + config.token}"
           onclick="openLink('{path()}?token=' + config.token)">{name}</a>

        <span class="priority">
            <button onclick="{mountSelect}" class="mdl-button mdl-js-button">
                {priority || 'NORMAL'}
            </button>
        </span>

        <div class="resizeable children" style="height: {30 * shownChildren}px">
            <tree_entry each="{values(children)}"></tree_entry>
        </div>
    </div>

    <style scoped>
        :scope {
            display: block;
            margin-left: 10px;
            padding-top: 10px;
            font-size: 14px;
        }

        span:hover {
            cursor: pointer;
        }

        .material-icons {
            font-size: 14px;
            color: #757575;
        }

        .children {
            overflow: hidden;
        }

        .resizeable {
            transition: height .2s cubic-bezier(.4, 0, .2, 1);
        }

        .mdl-button {
            line-height: 14px;
            height: 14px;
        }

        .priority {
            float: right;
        }
    </style>

    <script>
        var that = this;
        that.showChildren = false;
        that.childrenCount = Object.keys(that.children || {}).length;
        that.shownChildren = 0;
        that.hiddenChildren = that.childrenCount;

        this.on('mount', function () {
        });

        values = function (map) {
            var l = [];
            for (var key in map) {
                l.push(map[key]);
            }
            l.sort(function (a, b) {
                return a.id < b.id ? a : b
            });
//            log('after sort:')
//            log(l)
            return l;
        };
        that.updateShownChildsCount = function (innerChilds) {
//            log('entry.updateShownChildsCount ' + innerChilds + ' in ' + that.name);
            if (typeof(innerChilds) != "undefined")
                that.shownChildren += innerChilds;

            if (that.parent.updateShownChildsCount)
                that.parent.updateShownChildsCount(innerChilds);
        };

        that.toggleChildren = function (e) {
            if (e.processed)
                return;
            that.showChildren = !that.showChildren;
            log('toggleChildren: ' + that.showChildren + ' for element: ' + that.name);
//            log('parent: ' + that.parent.name);
            e.processed = true;
            var t = that.shownChildren;
            that.updateShownChildsCount(that.showChildren ? that.hiddenChildren : -that.shownChildren);
            that.hiddenChildren = t;
            that.update()
        };

        that.path = function () {
            return that.parent.path() + '/' + encodeURIComponent(that.name)
        };

        that.mountSelect = function (e) {
            var button = e.target;
            var l = ['OFF', 'NORMAL', 'HIGH'];

            mountMdlSelect(button, {
                items: l,
                onSelect: function (i, e) {
                    e.processed = true;
                    that.setPriority(l[i])
                }
            });
            e.processed = true;
        };

        that.setPriority = function (value) {
            if (that.isFolder) {
                var sp = function (entry) {
                    if (entry.children) {
                        for (var key in entry.children) {
                            sp(entry.children[key])
                        }
                    }
                    entry.priority = value;
                };
                sp(that);
            }

            that.parent.setPriorityForEntry(that.name, value);
            that.update();
        };

        that.setPriorityForEntry = function (entry, value) {
            that.children[entry].priority = value
        };

        openLink = function (url) {
            open(url, '_blank')
        };
    </script>
</tree_entry>