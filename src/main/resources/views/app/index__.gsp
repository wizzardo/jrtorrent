<%@ page import="com.wizzardo.jrt.TorrentInfo;" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>List of torrents</title>
    <style>
    .entries {
        padding-left: 20px;
    }
    </style>
</head>

<body>
<g:each in="${(List<TorrentInfo>) torrents}" var="torrent">
    <div>
        <strong>${torrent.name}</strong>
        <g:render template="../shared/torrentEntry"
                  model="[entries: ((Map<String, Collection>) entries).get(torrent.getHash())]"/>
    </div>
    <br/>
</g:each>
</body>
</html>