<add_button>
    <button class="mdl-button mdl-js-button" onclick="{show}">
        Add
    </button>

    <style scoped>
        :scope {
            display: inline;
        }

        .mdl-button {
            font-weight: bold;
            color: white;
        }
    </style>

    <script>
        var that = this;

        this.on('mount', function () {
           that.modal = riot.mount('#add_modal', {})[0]
        });

        that.show = function () {
            that.modal.show = true;
            that.modal.update();
        }
    </script>
</add_button>