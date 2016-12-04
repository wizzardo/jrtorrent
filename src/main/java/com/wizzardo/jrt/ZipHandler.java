package com.wizzardo.jrt;

import com.wizzardo.epoll.ByteBufferProvider;
import com.wizzardo.epoll.readable.ReadableBuilder;
import com.wizzardo.http.FileTreeHandler;
import com.wizzardo.http.HttpConnection;
import com.wizzardo.http.request.Header;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;
import com.wizzardo.tools.io.FileTools;
import com.wizzardo.tools.io.IOTools;
import com.wizzardo.tools.misc.Unchecked;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by wizzardo on 03/12/16.
 */
public class ZipHandler extends FileTreeHandler {

    public ZipHandler(File workDir, String prefix) {
        super(workDir, prefix);
    }

    public ZipHandler(File workDir, String prefix, String name) {
        super(workDir, prefix, name);
    }

    public ZipHandler(String workDir, String prefix, String name) {
        super(workDir, prefix, name);
    }

    public ZipHandler(String workDir, String prefix) {
        super(workDir, prefix);
    }

    @Override
    protected Response handleDirectory(Request request, Response response, String path, File file) {
        response.header(Header.KV_CONNECTION_CLOSE);
        response.header(Header.KEY_TRANSFER_ENCODING, Header.VALUE_CHUNKED);
        response.header(Header.KEY_CONTENT_TYPE, "application/zip");

        HttpConnection connection = request.connection();
        response.commit(connection);

        ChunkedReadableData readable = Unchecked.call(() -> new ChunkedReadableData(new ZipFolderBytesProducer(file), request.connection()));
        connection.write(readable, ((ByteBufferProvider) Thread.currentThread()));
        return response;
    }

    static class ZipFolderBytesProducer implements BytesProducer {

        protected static final byte[] JAR_MAGIC = new byte[]{(byte) 0xFE, (byte) 0xCA, 0, 0};

        byte[] buffer = new byte[16 * 1024];
        DirectByteArrayOutputStream out;
        ZipOutputStream zipout;
        Iterator<File> files;
        InputStream fileInput;
        String folderPath;

        ZipFolderBytesProducer(File folder) {
            out = new DirectByteArrayOutputStream(buffer.length);
            zipout = new ZipOutputStream(out);
            zipout.setMethod(ZipOutputStream.DEFLATED);
            zipout.setLevel(0);
            files = FileTools.listRecursive(folder).iterator();
            folderPath = folder.getParentFile().getAbsolutePath();
        }

        @Override
        public void produceTo(BytesConsumer consumer) throws IOException {
            if (fileInput == null) {
                zipout.closeEntry();

                if (files.hasNext()) {
                    File file = files.next();
                    fileInput = new FileInputStream(file);
                    ZipEntry entry = new ZipEntry(file.getAbsolutePath().substring(folderPath.length() + 1));
                    entry.setExtra(JAR_MAGIC); // jar magic here
                    zipout.putNextEntry(entry);
                    produceTo(consumer);
                } else {
                    zipout.close();
                    if (out.length() > 0)
                        consumer.consume(out.bytes(), 0, out.length());
                    consumer.consume(new byte[0], 0, 0);
                }
            } else {
                try {
                    int read = fileInput.read(buffer);
                    if (read > 0) {
                        zipout.write(buffer, 0, read);
                        if (out.length() > 0) {
                            consumer.consume(out.bytes(), 0, out.length());
                            out.reset();
                        } else {
                            produceTo(consumer);
                        }
                    } else {
                        IOTools.close(fileInput);
                        fileInput = null;
                        produceTo(consumer);
                    }
                } catch (IOException e) {
                    IOTools.close(fileInput);
                    throw Unchecked.rethrow(e);
                }
            }
        }
    }

    interface BytesProducer {
        void produceTo(BytesConsumer consumer) throws IOException;
    }

    interface BytesConsumer {
        void consume(byte[] bytes, int offset, int length);
    }

    static class ChunkedReadableData extends ReadableBuilder implements BytesConsumer {
        final static byte[] RN = "\r\n".getBytes(StandardCharsets.UTF_8);
        final BytesProducer producer;
        final HttpConnection connection;
        volatile boolean last;

        ChunkedReadableData(BytesProducer producer, HttpConnection connection) throws IOException {
            this.producer = producer;
            this.connection = connection;
            producer.produceTo(this);
        }

        @Override
        public void consume(byte[] bytes, int offset, int length) {
            if (length == 0)
                last = true;

            append(Integer.toHexString(length).getBytes(StandardCharsets.UTF_8));
            append(RN);
            append(bytes, offset, length);
            append(RN);
        }

        @Override
        public void onComplete() {
            if (!last) {
                ChunkedReadableData readable = Unchecked.call(() -> new ChunkedReadableData(producer, connection));
                if (readable.length() > 0)
                    connection.write(readable, ((ByteBufferProvider) Thread.currentThread()));
            } else
                Unchecked.run(connection::close);
        }
    }

    static class DirectByteArrayOutputStream extends ByteArrayOutputStream {
        public DirectByteArrayOutputStream(int size) {
            super(size);
        }

        byte[] bytes() {
            return buf;
        }

        int length() {
            return count;
        }
    }
}
