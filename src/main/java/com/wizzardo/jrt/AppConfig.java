package com.wizzardo.jrt;

import com.wizzardo.http.framework.Configuration;

import java.util.List;

public class AppConfig implements Configuration {

    public String downloads;
    public List<String> downloadsAliases;
    public List<String> folders;

    @Override
    public String prefix() {
        return "jrt";
    }
}
