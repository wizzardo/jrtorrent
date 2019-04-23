package com.wizzardo.jrt;

import com.wizzardo.http.TokenizedFileTreeHandler;
import com.wizzardo.http.filter.TokenFilter;
import com.wizzardo.http.framework.ControllerUrlMapping;
import com.wizzardo.http.framework.Holders;
import com.wizzardo.http.request.Header;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by wizzardo on 03/12/16.
 */
public class M3UHandler<T extends M3UHandler.HandlerContextWithRequest> extends TokenizedFileTreeHandler<T> {

    protected Set<String> extensions = new HashSet<>(Arrays.asList(
            "mkv",
            "avi",
            "mp4",
            "mp3",
            "aac",
            "flac",
            "ape",
            "wav",
            "ts"
    ));

    protected FileFilter fileFilter = pathname -> {
        if(pathname.isDirectory()) return true;
        String name = pathname.getName();
        int i = name.lastIndexOf('.');
        if (i == -1 || i == name.length() - 1)
            return false;
        String extension = name.substring(i + 1);
        return extensions.contains(extension.toLowerCase());
    };

    public M3UHandler(String workDir, String prefix, TokenFilter tokenFilter, String name) {
        super(workDir, prefix, tokenFilter, name);
    }

    @Override
    protected Response handleDirectory(Request request, Response response, String path, File file) {
        StringBuilder sb = new StringBuilder();
        T handlerContext = createHandlerContext(path, request);
        return response.setBody(addFileRecursively(file, sb, handlerContext).toString())
                .header(Header.KEY_CONTENT_TYPE, "audio/x-mpegurl")
                .header("Content-Disposition", "attachment; filename=\"" + file.getName() + ".m3u\"")
                ;
    }

    protected StringBuilder getPath(StringBuilder sb, File file) {
        if (!file.equals(workDir)) {
            getPath(sb, file.getParentFile());
            sb.append("/").append(encodeName(file.getName()));
        }
        return sb;
    }

    @Override
    protected String generateUrl(File file, T handlerContext) {
        ControllerUrlMapping mapping = Holders.getApplication().getUrlMapping();
        StringBuilder sb = new StringBuilder("https://" + handlerContext.request.header(Header.KEY_HOST));
        sb.append(mapping.getUrlTemplate("downloads").getRelativeUrl());
        getPath(sb, file.getParentFile()).append("/").append(super.generateUrl(file, handlerContext));
        return sb.toString();
    }

    protected StringBuilder addFileRecursively(File file, StringBuilder sb, T context) {
        if (file.isDirectory()) {
            File[] files = file.listFiles(fileFilter);
            if (files != null) {
                Arrays.sort(files);
                for (File f : files) {
                    addFileRecursively(f, sb, context);
                }
            }
        } else {
            sb.append(generateUrl(file, context)).append("\n");
        }
        return sb;
    }

    @Override
    protected T createHandlerContext(String path, Request request) {
        return (T) new HandlerContextWithRequest(path, request);
    }

    protected class HandlerContextWithRequest extends HandlerContextWithToken {
        protected final Request request;

        public HandlerContextWithRequest(String path, Request request) {
            super(path, request);
            this.request = request;
        }
    }
}
