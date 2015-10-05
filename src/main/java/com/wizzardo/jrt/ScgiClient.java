package com.wizzardo.jrt;

import com.wizzardo.http.HttpHeadersReader;
import com.wizzardo.tools.io.IOTools;
import com.wizzardo.tools.misc.Unchecked;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

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
