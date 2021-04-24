package com.wizzardo.jrt.bt;

import bt.data.*;
import bt.metainfo.Torrent;
import bt.metainfo.TorrentFile;
import bt.torrent.PieceStatistics;
import bt.torrent.selector.BaseStreamSelector;
import com.wizzardo.jrt.FilePriority;

import java.util.*;
import java.util.stream.Collectors;

public class PrioritizedSequentialPieceSelector extends BaseStreamSelector {

    private final Storage storage;
    private volatile List<TorrentFileWithPieces> filesWithPieces;

    public PrioritizedSequentialPieceSelector(Storage storage) {
        this.storage = storage;
    }

    protected PrimitiveIterator.OfInt createIterator(final PieceStatistics pieceStatistics) {
        List<TorrentFileWithPieces> files = this.filesWithPieces;
        return new PrimitiveIterator.OfInt() {
            int piece = 0;
            int fileIndex = 0;
            int pieceIndex = 0;

            public int nextInt() {
                return piece;
            }

            public boolean hasNext() {
                while (true) {
                    TorrentFileWithPieces f;
                    do {
                        if (fileIndex >= files.size())
                            return false;

                        f = files.get(fileIndex);
                        if (pieceIndex >= f.pieces.length) {
                            pieceIndex = 0;
                            fileIndex++;
                        } else {
                            break;
                        }
                    } while (true);
                    if (f.priority == FilePriority.OFF)
                        return false;

                    int piece = f.pieces[pieceIndex++];
                    if (pieceStatistics.getCount(piece) != 0) {
                        this.piece = piece;
                        return true;
                    }
                }
            }
        };
    }

    public void setPriority(String path, FilePriority priority) {
        String startsWith = path + "/";
        for (TorrentFileWithPieces file : filesWithPieces) {
            if (file.path.equals(path) || file.path.startsWith(startsWith))
                file.priority = priority;
        }

        List<TorrentFileWithPieces> newPriorities = new ArrayList<>(filesWithPieces);
        newPriorities.sort((o1, o2) -> {
            int priorityCompare = Integer.compare(o1.priority.value, o2.priority.value);
            if (priorityCompare == 0) {
                return o1.path.compareTo(o2.path);
            }
            return -priorityCompare;
        });
        filesWithPieces = newPriorities;
    }

    public void setTorrent(Torrent torrent) {
        List<TorrentFile> files = torrent.getFiles();
        Map<TorrentFile, List<Integer>> piecesByFile = new HashMap<>((int) (files.size() / 0.75d) + 1);
        Map<StorageUnit, TorrentFile> storageUnitsToFilesMap = new LinkedHashMap<>((int) (files.size() / 0.75d) + 1);
        files.forEach(f -> storageUnitsToFilesMap.put(storage.getUnit(torrent, f), f));

        long totalSize = torrent.getSize();
        long chunkSize = torrent.getChunkSize();

        List<StorageUnit> nonEmptyStorageUnits = storageUnitsToFilesMap.keySet().stream().filter(it -> it.capacity() > 0).collect(Collectors.toList());
        long limitInLastUnit = nonEmptyStorageUnits.get(nonEmptyStorageUnits.size() - 1).capacity();
        DataRange data = ReadWriteDataRangeHelper.create(nonEmptyStorageUnits, 0, limitInLastUnit);

        int pieceId = 0;
        long off, lim;
        long remaining = totalSize;
        while (remaining > 0) {
            off = pieceId * chunkSize;
            lim = Math.min(chunkSize, remaining);

            DataRange subrange = data.getSubrange(off, lim);

            int finalPieceId = pieceId;
            subrange.visitUnits((unit, off1, lim1) -> {
                piecesByFile.computeIfAbsent(storageUnitsToFilesMap.get(unit), torrentFile -> new ArrayList<>()).add(finalPieceId);
                return true;
            });

            pieceId++;
            remaining -= chunkSize;
        }

        filesWithPieces = files.stream().map(it ->
                new TorrentFileWithPieces(it, piecesByFile.get(it).stream().mapToInt(i -> i).toArray())
        ).collect(Collectors.toList());
    }

    static class TorrentFileWithPieces implements TorrentFile {
        final long size;
        final List<String> pathElements;
        final int[] pieces;
        final String path;
        FilePriority priority = FilePriority.NORMAL;

        TorrentFileWithPieces(TorrentFile torrentFile, int[] pieces) {
            this.size = torrentFile.getSize();
            this.pathElements = torrentFile.getPathElements();
            this.pieces = pieces;
            path = String.join("/", pathElements);
        }


        @Override
        public long getSize() {
            return size;
        }

        @Override
        public List<String> getPathElements() {
            return pathElements;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TorrentFileWithPieces)) return false;

            TorrentFileWithPieces that = (TorrentFileWithPieces) o;

            if (size != that.size) return false;
            return pathElements.equals(that.pathElements);
        }

        @Override
        public int hashCode() {
            int result = (int) (size ^ (size >>> 32));
            result = 31 * result + pathElements.hashCode();
            return result;
        }
    }
}
