package com.wizzardo.jrt;

import com.wizzardo.tools.misc.DateIso8601;
import com.wizzardo.tools.security.Base64;
import com.wizzardo.tools.xml.Node;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by wizzardo on 05.10.15.
 */
public class XmlRpc {
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
