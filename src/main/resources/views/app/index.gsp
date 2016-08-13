<%@ page import="com.wizzardo.http.framework.Holders; com.wizzardo.http.framework.Environment; com.wizzardo.jrt.TorrentInfo;" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="mdl_layout"/>
    <title>JRtorrent</title>
    <g:resource dir="js" file="lib.js"/>
    <g:resource dir="js" file="riot_compiler.js"/>
</head>

<body>

<div class="app">
    <torrents>torrents</torrents>
</div>

<add_button>add</add_button>

<modal id="add_modal">
    <upload_form>upload_form</upload_form>
</modal>

<modal id="delete_modal">
    <delete_form>delete_form</delete_form>
</modal>

<script src="${createLink(controller: 'app', action: 'tags')}" type="text/javascript"></script>

<script>
    var debug = false;
    var config = ${config};
    var handlers = {};
    var mdlMixin = {
        init: function () {
            var that = this;
            this.on('mount', function () {
//                log('on mount mixin');
                ['.mdl-textfield', '.mdl-menu', '.mdl-icon-toggle', '.mdl-button'].forEach(function (it) {
                    var l = that.root.querySelectorAll(it + ':not(.is-upgraded)');
                    for (i = 0; i < l.length; i++) {
                        var el = l[i];
                        el.dataset.upgraded = '';
                        componentHandler.upgradeAllRegistered(el);
                    }
                });
            })
        }
    };
    var obs;
    var deleteModal;

    handlers.list = function (data) {
        riot.mount('torrents', {torrents: data.torrents});
    };

    handlers.tree = function (data) {
        log('handlers.tree');
        log(data);
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

    handlers.callback = function (data) {
        if (obs)
            obs.trigger('callback.' + data.callbackId, data)
    };

    function initObserver() {
        log('initObserver');
        var obs = riot.observable();
        obs.on('load_tree', function (data) {
            log('load_tree ' + data.hash);
            sendCommand('loadTree', data)
        });
        obs.on('torrent.stop', function (data) {
            log('torrent.stop ' + data.hash);
            sendCommand('stop', data)
        });
        obs.on('torrent.start', function (data) {
            log('torrent.start ' + data.hash);
            sendCommand('start', data)
        });
        obs.on('torrent.delete', function (data) {
            log('torrent.delete ' + data.hash);
            obs.trigger('updateDeleteForm', data);
            deleteModal.toggle();
        });
        obs.on('sendDeleteTorrent', function (data) {
            log('sendDeleteTorrent ' + data.hash + " " + data.withData);
            sendCommand('delete', data)
        });
        obs.on('torrent.setPriority', function (data) {
            log('torrent.setPriority ' + data.hash + " " + data.path + " " + data.priority);
            sendCommand('setPriority', data)
        });
        obs.set = function (eventName, callback) {
            obs.off(eventName);
            obs.on(eventName, callback)
        };
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
            log("open");
            if (wsEvents.onOpen)
                wsEvents.onOpen();
        };
        ws.onmessage = function (e) {
//            log(e.data);
            var data = JSON.parse(e.data);
            handlers[data.command](data)
        };
        ws.onclose = function () {
            log("closed");
            if (wsEvents.onClose)
                wsEvents.onClose();
            connect();
        };
    }
    function log(message) {
        if (debug)
            console.log(message)
    }

    function mountMdlSelect(after, conf) {
        var id = after.id;
        if (!id) {
            id = ('r_' + Math.random()).replace('\.', '');
            after.id = id;
        }
        var select = document.createElement('mdl_select');
        select.id = 'mdl_select_' + id;
        if (after.nextSibling)
            after.parentNode.insertBefore(select, after.nextSibling);
        else
            after.parentNode.appendChild(select);

        conf['for'] = id;
        riot.mount(select, conf);
    }

    function registerCallback(callback) {
        var callbackId = Math.random() + "";
        obs.one('callback.' + callbackId, callback);
        return callbackId
    }

    riot.compile(function () {
        obs = initObserver();
        connect();
        riot.mount('add_button');
        deleteModal = riot.mount('#delete_modal')[0];
    });
    setInterval(function () {
        sendCommand('ping')
    }, 15000);
</script>
</body>
</html>