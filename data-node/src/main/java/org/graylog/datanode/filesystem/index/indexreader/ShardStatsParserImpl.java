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

import org.graylog.datanode.filesystem.index.IncompatibleIndexVersionException;
import org.graylog.datanode.filesystem.index.IndexerInformationParserException;
import org.graylog.shaded.opensearch2.org.apache.lucene.index.DirectoryReader;
import org.graylog.shaded.opensearch2.org.apache.lucene.index.IndexFormatTooOldException;
import org.graylog.shaded.opensearch2.org.apache.lucene.index.StandardDirectoryReader;
import org.graylog.shaded.opensearch2.org.apache.lucene.store.Directory;
import org.graylog.shaded.opensearch2.org.apache.lucene.store.FSDirectory;
import org.graylog.shaded.opensearch2.org.apache.lucene.util.Version;

import jakarta.inject.Singleton;

import java.io.IOException;
import java.nio.file.Path;

@Singleton
public class ShardStatsParserImpl implements ShardStatsParser {
    @Override
    public ShardStats read(Path shardPath) throws IncompatibleIndexVersionException {
        try (Directory directory = FSDirectory.open(shardPath.resolve("index"))) {
            final StandardDirectoryReader reader = (StandardDirectoryReader) DirectoryReader.open(directory);
            final int documentsCount = getDocumentsCount(reader);
            final Version minSegmentLuceneVersion = reader.getSegmentInfos().getMinSegmentLuceneVersion();
            return new ShardStats(shardPath, documentsCount, minSegmentLuceneVersion);
        } catch (IndexFormatTooOldException e) {
            throw new IncompatibleIndexVersionException(e);
        } catch (IOException e) {
            throw new IndexerInformationParserException("Failed to open index for read", e);
        }
    }

    private int getDocumentsCount(StandardDirectoryReader reader) {
        // use IndexSearcher if you want to count documents smarter, filtering by field or query
        // IndexSearcher searcher = new IndexSearcher(reader);
        return reader.numDocs();
    }
}
