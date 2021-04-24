package bt.torrent.data;

import bt.data.ChunkDescriptor;
import bt.data.ChunkVerifier;
import bt.data.DataDescriptor;
import bt.data.DefaultChunkVerifier;
import bt.metainfo.TorrentId;
import bt.net.Peer;
import bt.net.buffer.BufferedData;
import bt.net.buffer.ByteBufferView;
import bt.runtime.Config;
import bt.service.IRuntimeLifecycleBinder;
import bt.torrent.TorrentRegistry;
import bt.torrent.data.*;
import com.wizzardo.jrt.bt.ObservedChunkVerifier;
import com.wizzardo.tools.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class CachedDataWorker implements DataWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDataWorker.class);

    private static final Exception QUEUE_FULL_EXCEPTION = new IllegalStateException("Queue is overloaded");

    private final TorrentRegistry torrentRegistry;
    private final ChunkVerifier verifier;
    private final BlockCache blockCache;

    private final ExecutorService executor;
    private final int maxPendingTasks;
    private final AtomicInteger pendingTasksCount;
    private final WeakHashMap<Integer, byte[]> buffers = new WeakHashMap<>();

    Cache<Key, Entry> cache = new Cache<Key, Entry>(30).onRemove((k, v) -> {
        if (v.complete)
            return;

        for (int i = 0; i < v.blocks.size(); i++) {
            CompletableFuture<byte[]> block = v.blocks.get(i).future;
            if (block != null)
                block.completeExceptionally(new IllegalStateException("Timeout"));
        }
    });

    static class Key {
        final TorrentId torrentId;
        final int pieceIndex;

        Key(TorrentId torrentId, int pieceIndex) {
            this.torrentId = torrentId;
            this.pieceIndex = pieceIndex;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key)) return false;

            Key key = (Key) o;

            if (pieceIndex != key.pieceIndex) return false;
            return torrentId.equals(key.torrentId);
        }

        @Override
        public int hashCode() {
            int result = torrentId.hashCode();
            result = 31 * result + pieceIndex;
            return result;
        }
    }

    static class Entry {
        final List<Data> blocks;
        final long pieceLength;
        final int blocksCount;
        boolean complete;

        Entry(long pieceLength, int count) {
            this.pieceLength = pieceLength;
            blocks = new ArrayList<>(count);
            blocksCount = count;
        }

        public boolean isComplete() {
            return blocks.size() == blocksCount;
        }

        static class Data {
            final CompletableFuture<byte[]> future;
            final int offset;
            final BufferedData bufferedData;

            Data(CompletableFuture<byte[]> future, int offset, BufferedData bufferedData) {
                this.future = future;
                this.offset = offset;
                this.bufferedData = bufferedData;
            }
        }
    }


    public CachedDataWorker(IRuntimeLifecycleBinder lifecycleBinder,
                             TorrentRegistry torrentRegistry,
                             ChunkVerifier verifier,
                             BlockCache blockCache,
                             Config config) {

        this.torrentRegistry = torrentRegistry;
        this.verifier = verifier;
        this.blockCache = blockCache;

        this.executor = Executors.newSingleThreadExecutor(new ThreadFactory() {

            private AtomicInteger i = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, String.format("%d.bt.torrent.mydata.worker-%d", config.getAcceptorPort(), i.incrementAndGet()));
            }
        });
        this.maxPendingTasks = config.getMaxIOQueueSize();
        this.pendingTasksCount = new AtomicInteger();

        lifecycleBinder.onShutdown("Shutdown data worker", this.executor::shutdownNow);
    }

    @Override
    public CompletableFuture<BlockWrite> addBlock(TorrentId torrentId, Peer peer, int pieceIndex, int offset, BufferedData buffer) {
        Key key = new Key(torrentId, pieceIndex);

        DataDescriptor data = getDataDescriptor(torrentId);
        ChunkDescriptor chunk = data.getChunkDescriptors().get(pieceIndex);

        boolean isLastPiece = chunk.lastBlockSize() != chunk.blockSize();

        Entry entry = cache.get(key, k -> {
            if (isLastPiece)
                return new Entry(chunk.lastBlockSize() + (chunk.blockCount() - 1) * chunk.blockSize(), chunk.blockCount());
            else
                return new Entry(chunk.blockSize() * chunk.blockCount(), chunk.blockCount());
        });

        CompletableFuture<byte[]> future = new CompletableFuture<>();
        entry.blocks.add(new Entry.Data(future, offset, buffer));
        if (entry.isComplete()) {
            entry.complete = true;
            cache.remove(key);

            executor.execute(() -> {
                entry.blocks.sort(Comparator.comparingInt(value -> value.offset));
                byte[] bytes;
                int pieceLength = (int) entry.pieceLength;
                if (isLastPiece)
                    bytes = new byte[pieceLength];
                else {
                    bytes = buffers.computeIfAbsent(pieceLength, byte[]::new);
                }

                try {
                    byte[] b = null;
                    for (int i = 0; i < entry.blocks.size(); i++) {
                        Entry.Data d = entry.blocks.get(i);
                        ByteBufferView buf = d.bufferedData.buffer();

                        int p = buf.position();
                        if (b == null || b.length != buf.remaining())
                            b = new byte[buf.remaining()];

                        buf.get(b);
                        buf.position(p);

                        System.arraycopy(b, 0, bytes, d.offset, b.length);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                for (int i = 0; i < entry.blocks.size(); i++) {
                    Entry.Data d = entry.blocks.get(i);
                    d.future.complete(bytes);
                }
            });
        }

       return future.thenApply(bytes -> {
           try {
               if (offset != 0)
                   return BlockWrite.complete(peer, pieceIndex, offset, buffer.length(), null);

               boolean verified = ((ObservedChunkVerifier) verifier).verify(chunk, bytes);

               if (verified) {
                   chunk.getData().putBytes(bytes);
                   data.getBitfield().markVerified(pieceIndex);
               }
               CompletableFuture<Boolean> verificationFuture = CompletableFuture.completedFuture(verified);
               return BlockWrite.complete(peer, pieceIndex, offset, buffer.length(), verificationFuture);
           } catch (Throwable e) {
               return BlockWrite.exceptional(peer, e, pieceIndex, offset, buffer.length());
           } finally {
               pendingTasksCount.decrementAndGet();
               buffer.dispose();
           }
       });
    }


    @Override
    public CompletableFuture<BlockRead> addBlockRequest(TorrentId torrentId, Peer peer, int pieceIndex, int offset, int length) {
        DataDescriptor data = getDataDescriptor(torrentId);
        if (!data.getBitfield().isVerified(pieceIndex)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Rejecting request to read block because the piece is not verified yet:" +
                        " piece index {" + pieceIndex + "}, offset {" + offset + "}, length {" + length + "}, peer {" + peer + "}");
            }
            return CompletableFuture.completedFuture(BlockRead.rejected(peer, pieceIndex, offset, length));
        } else if (!tryIncrementTaskCount()) {
            LOGGER.warn("Rejecting request to read block because the queue is full:" +
                    " piece index {"+pieceIndex+"}, offset {"+offset+"}, length {"+length+"}, peer {"+peer+"}");
            return CompletableFuture.completedFuture(BlockRead.exceptional(peer,
                    QUEUE_FULL_EXCEPTION, pieceIndex, offset, length));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                BlockReader blockReader = blockCache.get(torrentId, pieceIndex, offset, length);
                return BlockRead.ready(peer, pieceIndex, offset, length, blockReader);
            } catch (Throwable e) {
                LOGGER.error("Failed to perform request to read block:" +
                        " piece index {" + pieceIndex + "}, offset {" + offset + "}, length {" + length + "}, peer {" + peer + "}", e);
                return BlockRead.exceptional(peer, e, pieceIndex, offset, length);
            } finally {
                pendingTasksCount.decrementAndGet();
            }
        }, executor);
    }


    private boolean tryIncrementTaskCount() {
        int newCount = pendingTasksCount.updateAndGet(oldCount -> {
            if (oldCount == maxPendingTasks) {
                return oldCount;
            } else {
                return oldCount + 1;
            }
        });
        return newCount < maxPendingTasks;
    }

    private DataDescriptor getDataDescriptor(TorrentId torrentId) {
        return torrentRegistry.getDescriptor(torrentId).get().getDataDescriptor();
    }
}
