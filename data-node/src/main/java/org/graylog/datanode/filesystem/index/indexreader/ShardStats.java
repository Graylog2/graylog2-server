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

import jakarta.annotation.Nullable;

import java.nio.file.Path;

/**
 * @param minSegmentLuceneVersion the oldest Lucene version among the shard's segments, {@code null} for a shard
 *                                without any segment. Kept as a plain string because the shard may have been read
 *                                by a different Lucene version than the one we compile against, and the two
 *                                {@code org.apache.lucene.util.Version} classes are unrelated types.
 */
public record ShardStats(Path path, int documentsCount, @Nullable String minSegmentLuceneVersion) {
}
