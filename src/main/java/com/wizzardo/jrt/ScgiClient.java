package com.wizzardo.jrt;

import com.wizzardo.http.HttpHeadersReader;
import com.wizzardo.tools.io.IOTools;
import com.wizzardo.tools.misc.DateIso8601;
import com.wizzardo.tools.misc.Stopwatch;
import com.wizzardo.tools.misc.Unchecked;
import com.wizzardo.tools.security.Base64;
import com.wizzardo.tools.xml.Node;
import com.wizzardo.tools.xml.XmlParser;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by wizzardo on 21.06.15.
 */
public class ScgiClient {

    public static final char NUL = 0;

    public static class Request {
        byte[] data;
        String host;
        int port;

        public Request(String host, int port, byte[] data) {
            this.host = host;
            this.port = port;
            this.data = data;
        }

        public Request(String host, int port, String data) {
            this(host, port, data.getBytes(StandardCharsets.UTF_8));
        }

        public String get() {

            byte[] head = ("CONTENT_LENGTH" + NUL + data.length + NUL
                    + "SCGI" + NUL + '1' + NUL).getBytes(StandardCharsets.UTF_8);
            try {
                Socket s = new Socket(host, port);
                OutputStream out = s.getOutputStream();

                out.write((String.valueOf(head.length) + ":").getBytes(StandardCharsets.UTF_8));
                out.write(head);
                out.write(',');
                out.write(data);

                byte[] bytes = IOTools.bytes(s.getInputStream());
                ScgiResponseReader responseReader = new ScgiResponseReader();
                int offset = responseReader.read(bytes);

                String response = new String(bytes, offset, bytes.length - offset);
                s.close();
                return response;
            } catch (Exception e) {
                throw Unchecked.rethrow(e);
            }
        }
    }

    static class ScgiResponseReader extends HttpHeadersReader {
        @Override
        protected int parseFirstLine(byte[] chars, int offset, int length) {
            return offset;
        }
    }

    public static class XmlRpc {
        String method;
        Params params = new Params();

        public XmlRpc(String method) {
            this(method, new Params());
        }

        public XmlRpc(String method, Params params) {
            this.method = method;
            this.params = params;
        }

        public XmlRpc(String method, String... params) {
            this.method = method;
            this.params = new Params();
            for (String param : params) {
                this.params.add(param);
            }
        }

        String render() {
            Node params = new Node("params");
            for (Param param : this.params.params) {
                params.add(param.render());
            }

            return new Node("methodCall")
                    .add(new Node("methodName").addText(method))
                    .add(params)
                    .toXML();
        }

        interface Param {
            void render(Node value);

            default Node value() {
                Node value = new Node("value");
                render(value);
                return value;
            }

            default Node render() {
                Node param = new Node("param");
                param.add(value());
                return param;
            }

            static Param from(int i) {
                return v -> v.add(new Node("int").addText(String.valueOf(i)));
            }

            static Param from(boolean b) {
                return v -> v.add(new Node("boolean").addText(b ? "1" : "0"));
            }

            static Param from(double d) {
                return v -> v.add(new Node("double").addText(String.valueOf(d)));
            }

            static Param from(String s) {
                if (s == null)
                    return nil();

                return v -> v.add(new Node("string").addText(String.valueOf(s)));
            }

            static Param from(byte[] bytes) {
                if (bytes == null)
                    return nil();

                return v -> v.add(new Node("base64").addText(Base64.encodeToString(bytes)));
            }

            static Param from(Date date) {
                if (date == null)
                    return nil();

                return v -> v.add(new Node("dateTime.iso8601").addText(DateIso8601.format(date)));
            }

            static Param from(List<Param> array) {
                if (array == null)
                    return nil();

                return v -> {
                    Node data = new Node("data");
                    v.add(new Node("array").add(data));
                    for (Param param : array) {
                        data.add(param.value());
                    }
                };
            }

            static Param from(Map<String, Param> map) {
                if (map == null)
                    return nil();

                return v -> {
                    Node struct = new Node("struct");
                    v.add(struct);

                    for (Map.Entry<String, Param> entry : map.entrySet()) {
                        struct.add(new Node("member")
                                        .add(new Node("name").addText(entry.getKey()))
                                        .add(entry.getValue().value())
                        );
                    }
                };
            }

            static Param from(XmlRpc rpc) {
                if (rpc == null)
                    return nil();

                return v -> {
                    Node struct = new Node("struct");
                    v.add(struct);

                    struct.add(new Node("member")
                            .add(new Node("name").addText("methodName"))
                            .add(Param.from(rpc.method).value()));
                    struct.add(new Node("member")
                            .add(new Node("name").addText("params"))
                            .add(Param.from(rpc.params.params).value()));
                };
            }

            static Param nil() {
                return v -> v.add(new Node("nil"));
            }
        }

        static class Params {
            List<Param> params = new ArrayList<>();

            Params add(int i) {
                params.add(Param.from(i));
                return this;
            }

            Params add(double d) {
                params.add(Param.from(d));
                return this;
            }

            Params add(boolean b) {
                params.add(Param.from(b));
                return this;
            }

            Params add(String s) {
                params.add(Param.from(s));
                return this;
            }

            Params add(byte[] bytes) {
                params.add(Param.from(bytes));
                return this;
            }

            Params add(Date date) {
                params.add(Param.from(date));
                return this;
            }

            Params add(Params params) {
                this.params.add(Param.from(params.params));
                return this;
            }

            Params add(XmlRpc xmlRpc) {
                this.params.add(Param.from(xmlRpc));
                return this;
            }
        }

    }

    public static void main(String[] args) throws IOException {
//        Node methodCall = new Node("methodCall")
//                .add(new Node("methodName").addText("get_directory"))
//                .add(new Node("params"));

//        Node methodCall = new Node("methodCall")
//                .add(new Node("methodName").addText("set_directory"))
//                .add(new Node("params")
//                        .add(new Node("param").add(new Node("value").add(new Node("string").addText("/tmp")))));
//        System.out.println(methodCall.toXML());
//        String request = methodCall.toXML();


//        String request = new XmlRpc("get_directory").render();
//        String request = new XmlRpc("get_directory", new XmlRpc.Params().add("/home/rtorrent")).render();
//        String request = new XmlRpc("get_directory", new XmlRpc.Params().add("/home/rtorrent")).render();
//        String request = new XmlRpc("system.listMethods").render();
//        String request = new XmlRpc("load_verbose", "/tmp/test.torrent").render();
//        String request = new XmlRpc("d.is_multi_file", "D4264E9D08C1F6BD9BCFC1D4B47E149C577D1FFC").render();
        //  String request = new XmlRpc("f.get_path", new XmlRpc.Params().add("D4264E9D08C1F6BD9BCFC1D4B47E149C577D1FFC").add(3)).render();
//        String request = new XmlRpc("f.set_priority", new XmlRpc.Params().add("D4264E9D08C1F6BD9BCFC1D4B47E149C577D1FFC").add(2).add(2)).render();
//        String request = new XmlRpc("d.get_size_files", new XmlRpc.Params().add("D4264E9D08C1F6BD9BCFC1D4B47E149C577D1FFC")).render();
//        String request = new XmlRpc("f.get_path_components", new XmlRpc.Params().add("D4264E9D08C1F6BD9BCFC1D4B47E149C577D1FFC").add(1)).render();
        String request = new XmlRpc("d.multicall", "main", "d.name=", "d.hash=", "d.size_bytes=", "d.bytes_done=", "d.get_down_rate=", "d.get_up_rate=", "d.get_complete=", "d.is_open=", "d.is_hash_checking=", "d.get_state=", "d.get_peers_complete=", "d.get_peers_accounted=", "d.get_up_total=").render();
//        String request = new XmlRpc("system.methodHelp", "load.raw").render();
//        String request = new XmlRpc("system.methodHelp", "d.set_custom").render();
//        String request = new XmlRpc("system.methodHelp", "f.path_components").render();
//        String request = new XmlRpc("system.methodSignature", "f.path_components").render();
        System.out.println(request);
        System.out.println("response: " + new Request("10.0.3.104", 5000, request).get());

//        String response = new XmlParser().parse(new Request("10.0.3.104", 5000, new XmlRpc("d.get_size_files", new XmlRpc.Params().add("D4264E9D08C1F6BD9BCFC1D4B47E149C577D1FFC")).render()).get())
//                .get("params/param/value/i8").text();
//
//
//        Stopwatch stopwatch = new Stopwatch("get list of files");
//        int l = Integer.parseInt(response);
////        for (int i = 0; i < l; i++) {
////            response = Node.parse(new Request("10.0.3.104", 5000, new XmlRpc("f.get_path", new XmlRpc.Params().add("D4264E9D08C1F6BD9BCFC1D4B47E149C577D1FFC").add(i)).render()).get())
////                    .get("params/param/value/string").text();
//////            System.out.println(response);
////        }
//        XmlRpc.Params params = new XmlRpc.Params();
//        params.add("D4264E9D08C1F6BD9BCFC1D4B47E149C577D1FFC")
//                .add(0)
//                .add("f.get_path_components=")
//                .add("f.get_priority=")
//                .add("f.get_completed_chunks=");
////        for (int i = 0; i < l; i++) {
////            params.add("f.get_path").add("D4264E9D08C1F6BD9BCFC1D4B47E149C577D1FFC").add(i);
////        }
//        System.out.println("response: " + new Request("10.0.3.104", 5000, new XmlRpc("f.multicall", params).render()).get());
//        System.out.println(stopwatch);
    }
}
