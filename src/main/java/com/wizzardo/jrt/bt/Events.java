package com.wizzardo.jrt.bt;

public enum Events {
    STOP_TORRENT,
    VERIFICATION_UPDATE,

    ;

    public static class VerificationUpdateData {
        public final String torrentHash;
        public final int processed, verified, total;

        public VerificationUpdateData(String torrentHash, int processed, int verified, int total) {
            this.torrentHash = torrentHash;
            this.processed = processed;
            this.verified = verified;
            this.total = total;
        }

        @Override
        public String toString() {
            return "VerificationUpdateData{" +
                    "torrentHash='" + torrentHash + '\'' +
                    ", processed=" + processed +
                    ", verified=" + verified +
                    ", total=" + total +
                    '}';
        }
    }
}
