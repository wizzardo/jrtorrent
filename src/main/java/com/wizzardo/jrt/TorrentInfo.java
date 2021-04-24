package com.wizzardo.jrt;

/**
 * Created by wizzardo on 03.10.15.
 */
public class TorrentInfo {
    private String name;
    private String hash;
    private long size;
    private long downloaded;
    private long uploaded;
    private long downloadSpeed;
    private long uploadSpeed;
    private long seeds;
    private long peers;
    private Status status;
    private int totalSeeds;
    private int totalPeers;

    public enum Status {
        SEEDING, FINISHED, DOWNLOADING, STOPPED, PAUSED, CHECKING, UNKNOWN;

        public static Status valueOf(boolean complete, boolean isOpen, boolean isHashChecking, boolean state) {
            if (complete && isOpen && !isHashChecking && state) {
                return SEEDING;
            } else if (complete && !isOpen && !isHashChecking && !state) {
                return FINISHED;
            } else if (!complete && isOpen && !isHashChecking && state) {
                return DOWNLOADING;
            } else if (!complete && !isOpen && !isHashChecking && state) {
                // stopped in the middle
                return STOPPED;
            } else if (!complete && !isOpen && !isHashChecking && !state) {
                // i dont know stopped
                return STOPPED;
            } else if (!complete && isOpen && !isHashChecking && !state) {
                return PAUSED;
            } else if (complete && isOpen && !isHashChecking && !state) {
                // seeding pause
                return PAUSED;
            } else if (complete && !isOpen && !isHashChecking && state) {
                return FINISHED;
            } else if (isHashChecking) {
                return CHECKING;
            }

            return UNKNOWN;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getDownloaded() {
        return downloaded;
    }

    public void setDownloaded(long downloaded) {
        this.downloaded = downloaded;
    }

    public long getDownloadSpeed() {
        return downloadSpeed;
    }

    public void setDownloadSpeed(long downloadSpeed) {
        this.downloadSpeed = downloadSpeed;
    }

    public long getUploadSpeed() {
        return uploadSpeed;
    }

    public void setUploadSpeed(long uploadSpeed) {
        this.uploadSpeed = uploadSpeed;
    }

    public long getSeeds() {
        return seeds;
    }

    public void setSeeds(long seeds) {
        this.seeds = seeds;
    }

    public long getPeers() {
        return peers;
    }

    public void setPeers(long peers) {
        this.peers = peers;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public long getUploaded() {
        return uploaded;
    }

    public void setUploaded(long uploaded) {
        this.uploaded = uploaded;
    }

    public int getTotalSeeds() {
        return totalSeeds;
    }

    public void setTotalSeeds(int totalSeeds) {
        this.totalSeeds = totalSeeds;
    }

    public int getTotalPeers() {
        return totalPeers;
    }

    public void setTotalPeers(int totalPeers) {
        this.totalPeers = totalPeers;
    }

    @Override
    public String toString() {
        return "TorrentInfo{" +
                "name='" + name + '\'' +
                ", hash='" + hash + '\'' +
                ", size=" + size +
                ", downloaded=" + downloaded +
                ", uploaded=" + uploaded +
                ", downloadSpeed=" + downloadSpeed +
                ", uploadSpeed=" + uploadSpeed +
                ", seeds=" + seeds +
                ", peers=" + peers +
                ", status=" + status +
                ", totalSeeds=" + totalSeeds +
                ", totalPeers=" + totalPeers +
                '}';
    }
}
