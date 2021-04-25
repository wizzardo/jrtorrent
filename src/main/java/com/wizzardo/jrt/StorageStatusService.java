package com.wizzardo.jrt;

import com.wizzardo.http.framework.Holders;
import com.wizzardo.http.framework.di.PostConstruct;
import com.wizzardo.http.framework.di.Service;

import java.io.File;

public class StorageStatusService extends Thread implements Service, PostConstruct {

    AppWebSocketHandler<?> appWebSocketHandler;
    File downloadsDir;
    volatile boolean pause = true;

    @Override
    public void init() {
        downloadsDir = new File(Holders.getConfig().config("jrt").get("downloads", "."));

        setDaemon(true);
        setName(StorageStatusService.class.getSimpleName());
        start();
    }

    @Override
    public void run() {
        while (true) {
            while (pause) {
                synchronized (this) {
                    while (pause) {
                        try {
                            this.wait();
                        } catch (InterruptedException ignored) {
                        }
                    }
                }
            }

            try {
                broadcast();
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void broadcast() {
        appWebSocketHandler.addBroadcastTask(new AppWebSocketHandler.DiskUsage(downloadsDir.getUsableSpace()));
    }

    public void pause() {
        pause = true;
    }

    public void unpause() {
        synchronized (this) {
            pause = false;
            this.notifyAll();
        }
    }
}
