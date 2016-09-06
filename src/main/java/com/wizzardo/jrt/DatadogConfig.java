package com.wizzardo.jrt;

import com.wizzardo.http.framework.Configuration;

/**
 * Created by wizzardo on 06/09/16.
 */
public class DatadogConfig implements Configuration {

    public boolean enabled;
    public String prefix = "";
    public String hostname;
    public int port;

    @Override
    public String prefix() {
        return "datadog";
    }
}
