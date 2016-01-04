<%@ page import="com.wizzardo.jrt.TorrentInfo;" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="mdl_layout"/>
    <title>JRtorrent</title>
    <g:resource dir="js" file="web_socket.js"/>
    <g:resource dir="js" file="lib.js"/>
    <g:resource dir="js" file="riot_compiler.js"/>
</head>

<body>

<div class="app">
    <torrents>torrents</torrents>
</div>

<modal id="add_modal">
    <upload_form>upload_form</upload_form>
</modal>

<script src="${createLink([mapping: 'static', path: "/js/tags/torrents.tag?${System.currentTimeMillis()}"])}" type="riot/tag"></script>
<script src="${createLink([mapping: 'static', path: "/js/tags/torrent.tag?${System.currentTimeMillis()}"])}" type="riot/tag"></script>
<script src="${createLink([mapping: 'static', path: "/js/tags/tree.tag?${System.currentTimeMillis()}"])}" type="riot/tag"></script>
<script src="${createLink([mapping: 'static', path: "/js/tags/tree_entry.tag?${System.currentTimeMillis()}"])}" type="riot/tag"></script>
<script src="${createLink([mapping: 'static', path: "/js/tags/modal.tag?${System.currentTimeMillis()}"])}" type="riot/tag"></script>
<script src="${createLink([mapping: 'static', path: "/js/tags/upload_form.tag?${System.currentTimeMillis()}"])}" type="riot/tag"></script>
<script src="${createLink([mapping: 'static', path: "/js/tags/add_button.tag?${System.currentTimeMillis()}"])}" type="riot/tag"></script>

<script>
    var config = ${config};
    var torrents = [];
    var torrentsByHash = {};
    var handlers = {};
    var mdlMixin = {
        init: function () {
            var that = this;
            this.on('mount', function () {
//                console.log('on mount mixin');
                var l = document.querySelectorAll(that.root.tagName.toLowerCase() + ' .mdl-textfield');
                for (i = 0; i < l.length; i++) {
                    var el = l[i];
//                    console.log('reregister ' + el);
                    el.dataset.upgraded = '';
                    componentHandler.upgradeAllRegistered(el);
                }
            })
        }
    };
    var obs;

    function mount() {
        torrentsTag = riot.mount('torrents', {torrents: torrents}, {}, function (tag) {
            torrentsTag = tag[0];
        });
        if (torrentsTag)
            torrentsTag = torrentsTag[0];
    }

    function find(hash) {
        return torrentsByHash[hash]
    }

    handlers.list = function (data) {
        obs = initObserver();
        torrentsByHash = {};
        for (var i = 0; i < data.torrents.length; i++) {
            data.torrents[i].obs = obs;
            torrentsByHash[data.torrents[i].hash] = data.torrents[i];
        }

        torrents = data.torrents;
        mount()
    };

    handlers.tree = function (data) {
        console.log('handlers.tree');
        console.log(data);
        obs.trigger('tree_loaded_' + data.hash, data.tree)
    };

    handlers.update = function (data) {
        if (obs)
            obs.trigger('update_' + data.torrent.hash, data.torrent)
    };

    function initObserver() {
        console.log('initObserver')
        var obs = riot.observable();
        obs.on('load_tree', function (data) {
            console.log('load_tree ' + data.hash);
            sendCommand('loadTree', data)
        });
        return obs;
    }

    var wsEvents = {
        onOpen: function () {
            sendCommand('list')
        }
    };

    function sendCommand(command, args) {
        ws.send(JSON.stringify({command: command, args: (args || {})}))
    }

    function connect() {
        ws = new WebSocket("ws://" + location.hostname + ":" + location.port + config.ws);
        ws.onopen = function () {
            console.log("open");
            if (wsEvents.onOpen)
                wsEvents.onOpen();
        };
        ws.onmessage = function (e) {
//            console.log(e.data);
            var data = JSON.parse(e.data);
            handlers[data.command](data)
        };
        ws.onclose = function () {
            console.log("closed");
            if (wsEvents.onClose)
                wsEvents.onClose();
            connect();
        };
    }
    connect();
    riot.mount('add_button');
</script>
</body>
</html>