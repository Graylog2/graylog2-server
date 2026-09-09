/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.datanode.filesystem.index.indexreader;

import jakarta.inject.Singleton;
import org.graylog.datanode.filesystem.index.IncompatibleIndexVersionException;
import org.graylog.datanode.filesystem.index.IndexerInformationParserException;
import org.graylog.shaded.lucene9.org.apache.lucene.index.IndexFormatTooOldException;
import org.graylog.shaded.lucene9.org.apache.lucene.index.SegmentCommitInfo;
import org.graylog.shaded.lucene9.org.apache.lucene.index.SegmentInfos;
import org.graylog.shaded.lucene9.org.apache.lucene.store.Directory;
import org.graylog.shaded.lucene9.org.apache.lucene.store.NIOFSDirectory;
import org.graylog.shaded.lucene9.org.apache.lucene.util.Version;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Reads shards that {@link ShardStatsParserImpl} can't open. Lucene supports only the current and the previous
 * major index format, so the Lucene we compile against is limited to Lucene 9 and 10 segments. This implementation
 * uses a relocated Lucene 9 ({@code org.graylog.shaded:lucene9}) and therefore additionally reads Lucene 8
 * segments, which is what opensearch 1.x and elasticsearch 7.x wrote.
 * <p>
 * Lucene 7 and older (elasticsearch 6.x and below) remains unreadable and still fails with
 * {@link IncompatibleIndexVersionException}.
 */
@Singleton
public class Lucene9ShardStatsParser implements ShardStatsParser {

    @Override
    public ShardStats read(Path shardPath) throws IncompatibleIndexVersionException {
        // NIOFSDirectory instead of FSDirectory.open(): the latter picks MMapDirectory, which loads its
        // IndexInput provider from the multi-release part of the Lucene jar. We only read a handful of small
        // metadata files once during startup, so plain NIO keeps the shaded jar's surface as small as possible.
        try (Directory directory = new NIOFSDirectory(shardPath.resolve("index"))) {
            final SegmentInfos segmentInfos = SegmentInfos.readLatestCommit(directory);
            return new ShardStats(shardPath, computeDocumentsCount(segmentInfos), minSegmentLuceneVersion(segmentInfos));
        } catch (IndexFormatTooOldException e) {
            throw new IncompatibleIndexVersionException(e);
        } catch (IOException e) {
            throw new IndexerInformationParserException("Failed to open index for read", e);
        }
    }

    private String minSegmentLuceneVersion(SegmentInfos segmentInfos) {
        return Optional.ofNullable(segmentInfos.getMinSegmentLuceneVersion()).map(Version::toString).orElse(null);
    }

    /**
     * Equivalent to {@code DirectoryReader.numDocs()}: sums live (non-hard-deleted) documents
     * across all segments. Soft deletes are not subtracted here, matching Lucene's own
     * {@code SegmentReader.numDocs()} which is based on the hard live-docs bitset.
     */
    private int computeDocumentsCount(SegmentInfos segmentInfos) {
        int count = 0;
        for (SegmentCommitInfo sci : segmentInfos) {
            count += sci.info.maxDoc() - sci.getDelCount();
        }
        return count;
    }
}
