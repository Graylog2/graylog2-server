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
import org.apache.lucene.index.IndexFormatTooOldException;
import org.apache.lucene.index.SegmentCommitInfo;
import org.apache.lucene.index.SegmentInfos;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.graylog.datanode.filesystem.index.IncompatibleIndexVersionException;
import org.graylog.datanode.filesystem.index.IndexerInformationParserException;

import java.io.IOException;
import java.nio.file.Path;

@Singleton
public class ShardStatsParserImpl implements ShardStatsParser {
    @Override
    public ShardStats read(Path shardPath) throws IncompatibleIndexVersionException {
        try (Directory directory = FSDirectory.open(shardPath.resolve("index"))) {
            // SegmentInfos.readLatestCommit reads only the segments_N file and per-segment
            // .si metadata files — it does not open any codec readers (stored fields, doc
            // values, postings, etc.), making it far cheaper than DirectoryReader.open().
            final SegmentInfos segmentInfos = SegmentInfos.readLatestCommit(directory);
            final int documentsCount = computeDocumentsCount(segmentInfos);
            final Version minSegmentLuceneVersion = segmentInfos.getMinSegmentLuceneVersion();
            return new ShardStats(shardPath, documentsCount, minSegmentLuceneVersion);
        } catch (IndexFormatTooOldException e) {
            throw new IncompatibleIndexVersionException(e);
        } catch (IOException e) {
            throw new IndexerInformationParserException("Failed to open index for read", e);
        }
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
