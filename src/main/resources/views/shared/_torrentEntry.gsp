<%@ page import="com.wizzardo.jrt.TorrentEntry" %>
<div class="entries">
    <g:each in="${(Collection<TorrentEntry>) entries}" var="entry">
        <g:if test="${entry.isFolder()}">
            ${entry.name}
            <g:render template="torrentEntry" model="[entries: entry.children.values()]"/>
        </g:if>
        <g:else>
            ${entry.name}
            <br/>
        </g:else>
    </g:each>
</div>