<tree_entry>
    <div onclick="{toggleChildren}">
        <div class="info">
            <div if="{isFolder}" class="folder">
                <i class="material-icons">{showChildren?'folder_open':'folder'}</i>
                <span class="folderName">{name}</span>

                <a href="#" class="zip" onclick="{openZip}">
                    <i class="material-icons">archive</i>
                </a>
            </div>

            <a if="{!isFolder}" href="#" onclick="{openDirectLink}">{name}</a>

        <span class="priority">
            <button onclick="{mountSelect}" class="mdl-button mdl-js-button">
                {priority || 'NORMAL'}
            </button>
        </span>
        </div>
        <div class="resizeable children" style="height: {30 * shownChildren}px; {shownChildren == 0 ? 'display:none' : ''}">
            <tree_entry each="{values(children)}"></tree_entry>
        </div>
    </div>

    <style>
        tree_entry {
            display: block;
            margin-left: 10px;
            padding-top: 10px;
            font-size: 14px;
        }

        tree_entry span:hover {
            cursor: pointer;
        }

        tree_entry .material-icons {
            font-size: 20px;
            color: #757575;
        }

        tree_entry .children {
            overflow: hidden;
        }

        tree_entry .resizeable {
            transition: height .2s cubic-bezier(.4, 0, .2, 1);
        }

        tree_entry .mdl-button {
            line-height: 20px;
            height: 20px;
            border-radius: 0px;
            webkit-transition: none;
            transition: none;
        }

        tree_entry .priority {
            float: right;
        }

        tree_entry .folder {
            display: inline-block;
        }

        tree_entry .info {
            line-height: 20px;
        }

        tree_entry .info:hover {
            background-color: rgba(0, 0, 0, 0.075);
        }

        tree_entry .folderName {
            display: inline-block;
            margin-left: 5px;
        }

        tree_entry .zip {
            margin-left: 10px;
        }

        @media screen and (max-width: 736px) {
            tree_entry .priority {
                display: none;
            }
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
                return a.name < b.name ? -1 : (a.name > b.name ? 1 : 0)
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

        that.link = function () {
            return config.downloadsPath + that.path() + '?token=' + config.token;
        };

        that.zip = function () {
            return config.zipPath + that.path() + '?token=' + config.token;
        };

        that.path = function () {
            return that.parent.path() + '/' + encodeURIComponent(that.name)
        };

        that.hash = function () {
            return that.parent.hash()
        };

        that.mountSelect = function (e) {
            var button = e.target;
            var l = ['OFF', 'NORMAL', 'HIGH'];

            mountMdlSelect(button, {
                items: l,
                onSelect: function (i, e) {
                    e.processed = true;
                    var priority = l[i];
                    obs.trigger('torrent.setPriority', {
                        hash: that.hash(),
                        path: that.path(),
                        priority: priority,
                        callbackId: registerCallback(function () {
                            that.setPriority(priority);
                            log('set priority to ' + priority + ' for ' + that.path() + ' and hash:' + that.hash());
                        })
                    })
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
            that.parent.update();
        };

        that.setPriorityForEntry = function (entry, value) {
            that.children[entry].priority = value
        };

        that.openDirectLink = function (e) {
            openUrl(that.link(), e);
        };
        that.openZip = function (e) {
            openUrl(that.zip(), e);
        };
        openUrl = function (url, e) {
            e.processed = true;
            open(url, '_blank');
        };
    </script>
</tree_entry>