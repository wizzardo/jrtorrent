<tree>
    <tree_entry each={opts.entries}></tree_entry>

    <style scoped>
        :scope {
            display: block;
            transition: max-height .2s cubic-bezier(.4,0,.2,1);
            overflow: hidden;
        }
    </style>

    <script>
        var that = this;
        var opened = false;
        var hash = opts.hash;

        console.log('create tree tag with hash: ' + hash);

        this.on('mount', function () {
        });

    </script>
</tree>