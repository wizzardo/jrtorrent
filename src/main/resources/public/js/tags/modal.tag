<modal>

    <div class="overlay" style="top: {show?0:-100}%; right: {show?0:-100}%">
        <div class="content">
            <yield/>
        </div>
    </div>

    <style scoped>
        :scope {
            display: block;
        }

        .overlay {
            transition: top .2s cubic-bezier(.4, 0, .2, 1), right .2s cubic-bezier(.4, 0, .2, 1);
            position: fixed;
            width: 100%;
            height: 100%;
            z-index: 100;
            background-color: rgba(0, 0, 0, 0.5);
        }

        .content {
            width: 300px;
            height: 200px;
            background-color: white;
            margin-left: auto;
            margin-right: auto;
            font-size: 16px;
            min-width: 600px;
            max-width: 90%;
            margin-top: 10%;
            padding: 20px;
            box-shadow: 1px 1px 4px 0px rgba(0, 0, 0, 0.75);
        }
    </style>

    <script>
        var that = this;
        this.on('mount', function () {
        });
    </script>

</modal>