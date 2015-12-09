<%@ page import="com.wizzardo.jrt.TorrentInfo;" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>JRtorrent</title>
    <g:resource dir="js" file="web_socket.js"/>
    <g:resource dir="js" file="lib.js"/>
    <g:resource dir="js" file="riot_compiler.js"/>
</head>

<body>

<div class="app">
    <torrents>!</torrents>
</div>

<script src="/static/js/tags/torrents.tag" type="riot/tag"></script>
<script src="/static/js/tags/torrent.tag" type="riot/tag"></script>
<script>
    var torrents = [];
    var torrentsByHash = {};
    var handlers = {};

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
        obs = riot.observable();
        for (var i = 0; i < data.torrents.length; i++) {
            data.torrents[i].obs = obs
            torrentsByHash[data.torrents[i].hash] = data.torrents[i]
        }

        torrents = data.torrents;
        mount()
    };

    handlers.update = function (data) {
        var torrent = find(data.torrent.hash);
        obs.trigger('update_progress_' + data.torrent.hash, data.torrent.progress, function () {
            torrent.progress = data.torrent.progress
        })
    };

    var wsEvents = {
        onOpen: function () {
            ws.send('{"command":"list"}');
        }
    };

    function connect() {
        ws = new WebSocket("ws://" + location.hostname + ":" + location.port + "/ws");
        ws.onopen = function () {
            console.log("open");
            if (wsEvents.onOpen)
                wsEvents.onOpen();
        };
        ws.onmessage = function (e) {
            console.log(e.data);
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
</script>
</body>
</html>