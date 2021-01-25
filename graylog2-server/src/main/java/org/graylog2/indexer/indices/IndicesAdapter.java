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
package org.graylog2.indexer.indices;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.joschi.jadconfig.util.Duration;
import org.graylog2.indexer.indices.stats.IndexStatistics;
import org.graylog2.indexer.searches.IndexRangeStats;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public interface IndicesAdapter {
    void move(String source, String target, Consumer<IndexMoveResult> resultCallback);

    void delete(String indexName);

    Set<String> resolveAlias(String alias);

    void create(String indexName, IndexSettings indexSettings, String templateName, Map<String, Object> template);

    boolean ensureIndexTemplate(String templateName, Map<String, Object> template);

    Optional<DateTime> indexCreationDate(String index);

    void openIndex(String index);

    void setReadOnly(String index);

    void flush(String index);

    void markIndexReopened(String index);

    void removeAlias(String indexName, String alias);

    void close(String indexName);

    long numberOfMessages(String indexName);

    boolean aliasExists(String alias) throws IOException;

    Map<String, Set<String>> aliases(String indexPattern);

    boolean deleteIndexTemplate(String templateName);

    Map<String, Set<String>> fieldsInIndices(String[] writeIndexWildcards);

    Set<String> closedIndices(Collection<String> indices);

    Set<IndexStatistics> indicesStats(Collection<String> indices);

    Optional<IndexStatistics> getIndexStats(String index);

    JsonNode getIndexStats(Collection<String> index);

    boolean exists(String indexName) throws IOException;

    Set<String> indices(String indexWildcard, List<String> status, String id);

    Optional<Long> storeSizeInBytes(String index);

    void cycleAlias(String aliasName, String targetIndex);

    void cycleAlias(String aliasName, String targetIndex, String oldIndex);

    void removeAliases(Set<String> indices, String alias);

    void optimizeIndex(String index, int maxNumSegments, Duration timeout);

    IndexRangeStats indexRangeStatsOfIndex(String index);

    HealthStatus waitForRecovery(String index);
    HealthStatus waitForRecovery(String index, int timeout);

    boolean isOpen(String index);

    boolean isClosed(String index);
}
