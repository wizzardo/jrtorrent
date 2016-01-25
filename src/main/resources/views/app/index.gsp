<%@ page import="com.wizzardo.http.framework.Holders; com.wizzardo.http.framework.Environment; com.wizzardo.jrt.TorrentInfo;" contentType="text/html;charset=UTF-8" %>
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

<modal id="delete_modal">
    <delete_form>delete_form</delete_form>
</modal>

<g:set var="noCache" value="${Holders.environment == Environment.DEVELOPMENT ? [r: System.currentTimeMillis()] : [:]}" />

<script src="${createLink([mapping: 'static', params: noCache, path: "/js/tags/torrents.tag"])}" type="riot/tag"></script>
<script src="${createLink([mapping: 'static', params: noCache, path: "/js/tags/torrent.tag"])}" type="riot/tag"></script>
<script src="${createLink([mapping: 'static', params: noCache, path: "/js/tags/tree.tag"])}" type="riot/tag"></script>
<script src="${createLink([mapping: 'static', params: noCache, path: "/js/tags/tree_entry.tag"])}" type="riot/tag"></script>
<script src="${createLink([mapping: 'static', params: noCache, path: "/js/tags/modal.tag"])}" type="riot/tag"></script>
<script src="${createLink([mapping: 'static', params: noCache, path: "/js/tags/upload_form.tag"])}" type="riot/tag"></script>
<script src="${createLink([mapping: 'static', params: noCache, path: "/js/tags/delete_form.tag"])}" type="riot/tag"></script>
<script src="${createLink([mapping: 'static', params: noCache, path: "/js/tags/add_button.tag"])}" type="riot/tag"></script>

<script>
    var config = ${config};
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
    var deleteModal;

    handlers.list = function (data) {
        torrentsTag = riot.mount('torrents', {torrents: data.torrents}, {}, function (tags) {
            torrentsTag = tags[0];
        });
        if (torrentsTag)
            torrentsTag = torrentsTag[0];
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

    handlers.add = function (data) {
        if (obs)
            obs.trigger('addTorrent', data.torrent)
    };

    handlers.remove = function (data) {
        if (obs)
            obs.trigger('removeTorrent', data.torrent)
    };

    function initObserver() {
        console.log('initObserver');
        var obs = riot.observable();
        obs.on('load_tree', function (data) {
            console.log('load_tree ' + data.hash);
            sendCommand('loadTree', data)
        });
        obs.on('torrent.stop', function (data) {
            console.log('torrent.stop ' + data.hash);
            sendCommand('stop', data)
        });
        obs.on('torrent.start', function (data) {
            console.log('torrent.start ' + data.hash);
            sendCommand('start', data)
        });
        obs.on('torrent.delete', function (data) {
            console.log('torrent.delete ' + data.hash);
            obs.trigger('updateDeleteForm', data);
            deleteModal.toggle();
        });
        obs.on('sendDeleteTorrent', function (data) {
            console.log('sendDeleteTorrent ' + data.hash + " " + data.withData);
            sendCommand('delete', data)
        });
        return obs;
    }

    var wsEvents = {
        onOpen: function () {
            sendCommand('list');
        }
    };

    function sendCommand(command, args) {
        ws.send(JSON.stringify({command: command, args: (args || {})}))
    }

    function connect() {
        var https = location.protocol === 'https:';
        var port = location.port || (https ? 443 : 80);
        ws = new WebSocket((https ? 'wss' : 'ws') + "://" + location.hostname + ":" + port + config.ws + '?token=' + config.token);
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

    riot.compile(function() {
        obs = initObserver();
        connect();
        riot.mount('add_button');
        deleteModal = riot.mount('#delete_modal')[0];
    })
</script>
</body>
</html>