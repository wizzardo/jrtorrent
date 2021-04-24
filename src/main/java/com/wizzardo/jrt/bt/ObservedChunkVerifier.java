package com.wizzardo.jrt.bt;

import bt.BtException;
import bt.data.*;
import bt.data.digest.Digester;
import bt.data.range.ByteRange;
import bt.data.range.Range;
import bt.net.buffer.ByteBufferView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class ObservedChunkVerifier implements ChunkVerifier {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultChunkVerifier.class);

    private Digester digester;
    private int numOfHashingThreads;
    final ExecutorService workers;

    public ObservedChunkVerifier(Digester digester, int numOfHashingThreads) {
        this.digester = digester;
        this.numOfHashingThreads = numOfHashingThreads;
        if (numOfHashingThreads > 1)
            workers = Executors.newFixedThreadPool(numOfHashingThreads);
        else
            workers = null;
    }

    @Override
    public boolean verify(List<ChunkDescriptor> chunks, Bitfield bitfield) {
        return verify(chunks, bitfield, null);
    }

    public boolean verify(List<ChunkDescriptor> chunks, Bitfield bitfield, Listener listener) {
        if (chunks.size() != bitfield.getPiecesTotal()) {
            throw new IllegalArgumentException("Bitfield has different size than the list of chunks. Bitfield size: " +
                    bitfield.getPiecesTotal() + ", number of chunks: " + chunks.size());
        }

        ChunkDescriptor[] arr = chunks.toArray(new ChunkDescriptor[chunks.size()]);
        if (numOfHashingThreads > 1) {
            collectParallel(arr, bitfield, listener);
        } else {
            AtomicInteger verified = new AtomicInteger();
            AtomicInteger processed = new AtomicInteger();
            createWorker(arr, 0, arr.length, bitfield, processed, verified, listener).run();
        }
        // try to purge all data that was loaded by the verifiers
        System.gc();

        return bitfield.getPiecesRemaining() == 0;
    }

    @Override
    public boolean verify(ChunkDescriptor chunk) {
        byte[] expected = chunk.getChecksum();
        byte[] actual = digester.digestForced(chunk.getData());
        return Arrays.equals(expected, actual);
    }

    static class BytesRange implements Range<BytesRange> {
        private final byte[] bytes;

        BytesRange(byte[] bytes) {
            this.bytes = bytes;
        }

        @Override
        public long length() {
            return bytes.length;
        }

        @Override
        public Range<BytesRange> getSubrange(long offset, long length) {
            byte[] bytes = new byte[(int) length];
            System.arraycopy(this.bytes, (int) offset, bytes, 0, bytes.length);
            return new BytesRange(bytes);
        }

        @Override
        public Range<BytesRange> getSubrange(long l) {
            return null;
        }

        @Override
        public byte[] getBytes() {
            return bytes;
        }

        @Override
        public boolean getBytes(ByteBuffer byteBuffer) {
            return false;
        }

        @Override
        public void putBytes(byte[] bytes) {

        }

        @Override
        public void putBytes(ByteBufferView byteBufferView) {

        }
    }

    public boolean verify(ChunkDescriptor chunk, byte[] bytes) {
        byte[] expected = chunk.getChecksum();
        byte[] actual = digester.digest(new BytesRange(bytes));
        return Arrays.equals(expected, actual);
    }

    @Override
    public boolean verifyIfPresent(ChunkDescriptor chunk) {
        byte[] expected = chunk.getChecksum();
        byte[] actual = digester.digest(chunk.getData());
        return Arrays.equals(expected, actual);
    }

    private List<ChunkDescriptor> collectParallel(ChunkDescriptor[] chunks, Bitfield bitfield, Listener listener) {
        int n = numOfHashingThreads;

        List<Future<?>> futures = new ArrayList<>();

        int batchSize = chunks.length / n;
        int i, limit = 0;
        AtomicInteger verified = new AtomicInteger();
        AtomicInteger processed = new AtomicInteger();
        while ((i = limit) < chunks.length) {
            if (futures.size() == n - 1) {
                // assign the remaining bits to the last worker
                limit = chunks.length;
            } else {
                limit = i + batchSize;
            }
            futures.add(workers.submit(createWorker(chunks, i, Math.min(chunks.length, limit), bitfield, processed, verified, listener)));
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Verifying torrent data with {} workers", futures.size());
        }

        Set<Throwable> errors = ConcurrentHashMap.newKeySet();
        futures.forEach(f -> {
            try {
                f.get();
            } catch (Exception e) {
                LOGGER.error("Unexpected error during verification of torrent data", e);
                errors.add(e);
            }
        });

//        workers.shutdown();
//        while (!workers.isTerminated()) {
//            try {
//                Thread.sleep(100);
//            } catch (InterruptedException e) {
//                throw new BtException("Unexpectedly interrupted");
//            }
//        }

        if (!errors.isEmpty()) {
            throw new BtException("Failed to verify torrent data:" +
                    errors.stream().map(this::errorToString).reduce(String::concat).get());
        }

        return Arrays.asList(chunks);
    }

    public interface Listener {
        boolean onUpdate(int processed, int verified, int total);
    }

    private Runnable createWorker(ChunkDescriptor[] chunks,
                                  int from,
                                  int to,
                                  Bitfield bitfield,
                                  AtomicInteger processedCount,
                                  AtomicInteger verifiedCount,
                                  Listener listener) {
        return () -> {
            int i = from;
            while (i < to) {
                // optimization to speedup the initial verification of torrent's data
                int[] emptyUnits = new int[]{0};
                chunks[i].getData().visitUnits((u, off, lim) -> {
                    // limit of 0 means an empty file,
                    // and we don't want to account for those
                    if (u.size() == 0 && lim != 0) {
                        emptyUnits[0]++;
                    }
                    return true;
                });

                // if any of this chunk's storage units is empty,
                // then the chunk is neither complete nor verified
                if (emptyUnits[0] == 0) {
                    boolean verified = verifyIfPresent(chunks[i]);
                    if (verified) {
                        bitfield.markVerified(i);
                        verifiedCount.incrementAndGet();
                    }
                }
                i++;

                if (listener != null && !listener.onUpdate(processedCount.incrementAndGet(), verifiedCount.get(), chunks.length))
                    break;
            }
        };
    }

    private String errorToString(Throwable e) {
        StringBuilder buf = new StringBuilder();
        buf.append("\n");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bos);
        e.printStackTrace(out);

        buf.append(bos.toString());
        return buf.toString();
    }
}
