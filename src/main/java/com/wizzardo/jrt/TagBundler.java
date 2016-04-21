package com.wizzardo.jrt;

import com.wizzardo.http.framework.Holders;
import com.wizzardo.http.framework.di.DependencyScope;
import com.wizzardo.http.framework.di.Injectable;
import com.wizzardo.http.framework.template.ResourceTools;
import com.wizzardo.tools.cache.Cache;
import com.wizzardo.tools.io.FileTools;
import com.wizzardo.tools.misc.Unchecked;
import com.wizzardo.tools.xml.HtmlParser;
import com.wizzardo.tools.xml.Node;

import java.io.IOException;
import java.util.Map;

/**
 * Created by wizzardo on 03.04.16.
 */
@Injectable(scope = DependencyScope.SINGLETON)
public class TagBundler {

    private ResourceTools resourceTools;
    private Cache<String, String> cache = new Cache<>(
            Holders.getConfig().config("tagBundler").get("ttl", 1),
            tag -> asJavascript(resourceTools.getResourceAsString("/public/js/tags/" + tag + ".tag"))
    );

    public String toJavascript(String tag) {
        return cache.get(tag);
    }

    private static String asJavascript(String tag) throws IOException {
        Node node = Unchecked.call(() -> new HtmlParser().parse(tag));

        StringBuilder html = new StringBuilder();
        for (Node child : node.get(0).children()) {
            if ("script".equals(child.name()))
                continue;
            if ("style".equals(child.name()))
                continue;

            String s = render(child, new StringBuilder()).toString();

            s = s.replaceAll("[\r\n ]+", " ");
            s = s.replaceAll("'", "\\\\'");
            s = s.replaceAll("&apos;", "\\\\'");

            if (html.length() != 0)
                html.append(' ');
            html.append(s);
        }

        Node style = node.get(0).get("style");
        Node scriptNode = node.get(0).get("script");
        String css = style.text().replaceAll("[\r\n ]+", " ").replaceAll("'", "\\\\'").trim();

        String script = scriptNode.text().trim();
        return "riot.tag('" + node.get(0).name() + "'" +
                ",\n'" + html + "'" +
                ",\n'" + css + "'" +
                ",\nfunction (opts) {\n\t" + script + "\n\t}\n" +
                ");\n"
                ;
    }

    public static StringBuilder render(Node n, StringBuilder l) {
        if (n.isComment())
            return l;

        if (n.name() == null) {
            l.append(n.textOwn());
            return l;
        }

        l.append("<").append(n.name());
        for (Map.Entry<String, String> attr : n.attributes().entrySet()) {
            l.append(" ");

            l.append(attr.getKey());

            String value = attr.getValue();
            if (value != null) {
                l.append("=\"").append(value).append("\"");
            }
        }
        if (!n.isEmpty()) {
            l.append(">");
            for (Node child : n.children()) {
                render(child, l);
            }
            l.append("</").append(n.name()).append(">");
        } else if (n.name().equalsIgnoreCase("div"))
            l.append("></").append(n.name()).append(">");
        else
            l.append("/>");

        return l;
    }
}
