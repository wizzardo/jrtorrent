<tree_entry>
    <div onclick="{toggleChildren}">
        {name}
        <div class="children" show="{showChildren}">
            <tree_entry each="{values(children)}"></tree_entry>
        </div>
    </div>

    <style scoped>
        :scope {
            display: block;
            margin-left: 10px;
        }
    </style>

    <script>
        var that = this;
        that.showChildren = false;

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
//            console.log('after sort:')
//            console.log(l)
            return l;
        };

        that.toggleChildren = function (e) {
            if (e.processed)
                return;
            that.showChildren = !that.showChildren;
//            console.log('toggleChildren: ' + that.showChildren + ' for element: ' + that.name);
//            console.log('parent: ' + that.parent.name);
            e.processed = true;
            that.update()
        }
    </script>
</tree_entry>