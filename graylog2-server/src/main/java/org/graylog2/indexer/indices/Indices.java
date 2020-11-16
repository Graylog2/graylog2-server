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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import org.graylog2.audit.AuditActor;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.indexer.IndexMappingFactory;
import org.graylog2.indexer.IndexMappingTemplate;
import org.graylog2.indexer.IndexNotFoundException;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indices.events.IndicesClosedEvent;
import org.graylog2.indexer.indices.events.IndicesDeletedEvent;
import org.graylog2.indexer.indices.events.IndicesReopenedEvent;
import org.graylog2.indexer.indices.stats.IndexStatistics;
import org.graylog2.indexer.searches.IndexRangeStats;
import org.graylog2.plugin.system.NodeId;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.graylog2.audit.AuditEventTypes.ES_INDEX_CREATE;

@Singleton
public class Indices {
    private static final Logger LOG = LoggerFactory.getLogger(Indices.class);
    public static final String REOPENED_ALIAS_SUFFIX = "_reopened";

    private final IndexMappingFactory indexMappingFactory;
    private final NodeId nodeId;
    private final AuditEventSender auditEventSender;
    @SuppressWarnings("UnstableApiUsage")
    private final EventBus eventBus;
    private final IndicesAdapter indicesAdapter;

    @Inject
    public Indices(IndexMappingFactory indexMappingFactory,
                   NodeId nodeId,
                   AuditEventSender auditEventSender,
                   @SuppressWarnings("UnstableApiUsage") EventBus eventBus,
                   IndicesAdapter indicesAdapter) {
        this.indexMappingFactory = indexMappingFactory;
        this.nodeId = nodeId;
        this.auditEventSender = auditEventSender;
        this.eventBus = eventBus;
        this.indicesAdapter = indicesAdapter;
    }

    public void move(String source, String target) {
        indicesAdapter.move(source, target, (result) -> {
            LOG.info("Moving index <{}> to <{}>: Bulk indexed {} messages, took {} ms, failures: {}",
                    source,
                    target,
                    result,
                    result.tookMs(),
                    result.hasFailedItems());

            if (result.hasFailedItems()) {
                throw new ElasticsearchException("Failed to move a message. Check your indexer log.");
            }
        });
    }

    public void delete(String indexName) {
        indicesAdapter.delete(indexName);
        //noinspection UnstableApiUsage
        eventBus.post(IndicesDeletedEvent.create(indexName));
    }

    public void close(String indexName) {
        if (isReopened(indexName)) {
            indicesAdapter.removeAlias(indexName, indexName + REOPENED_ALIAS_SUFFIX);
        }
        indicesAdapter.close(indexName);
        //noinspection UnstableApiUsage
        eventBus.post(IndicesClosedEvent.create(indexName));
    }

    public long numberOfMessages(String indexName) throws IndexNotFoundException {
        return indicesAdapter.numberOfMessages(indexName);
    }

    public JsonNode getIndexStats(final IndexSet indexSet) {
        return indicesAdapter.getIndexStats(Collections.singleton(indexSet.getIndexWildcard()));
    }

    public boolean exists(String indexName) {
        try {
            return indicesAdapter.exists(indexName);
        } catch (IOException e) {
            throw new ElasticsearchException("Couldn't check existence of index " + indexName, e);
        }
    }

    public boolean aliasExists(String alias) {
        try {
            return indicesAdapter.aliasExists(alias);
        } catch (IOException e) {
            throw new ElasticsearchException("Couldn't check existence of alias " + alias, e);
        }
    }

    /**
     * Returns index names and their aliases. This only returns indices which actually have an alias.
     */
    @NotNull
    public Map<String, Set<String>> getIndexNamesAndAliases(String indexPattern) {
        return indicesAdapter.aliases(indexPattern);
    }

    public Optional<String> aliasTarget(String alias) throws TooManyAliasesException {
        final Set<String> indices = indicesAdapter.resolveAlias(alias);
        if (indices.size() > 1) {
            throw new TooManyAliasesException(indices);
        }

        return indices.stream().findFirst();
    }

    public void ensureIndexTemplate(IndexSet indexSet) {
        final IndexSetConfig indexSetConfig = indexSet.getConfig();
        final String templateName = indexSetConfig.indexTemplateName();
        final Map<String, Object> template = buildTemplate(indexSet, indexSetConfig);

        final boolean result = indicesAdapter.ensureIndexTemplate(templateName, template);

        if (result) {
            LOG.info("Successfully created index template {}", templateName);
        }
    }

    public Map<String, Object> getIndexTemplate(IndexSet indexSet) {
        final String indexWildcard = indexSet.getIndexWildcard();

        return indexMappingFactory.createIndexMapping(
                indexSet.getConfig()
                        .indexTemplateType()
                        .orElse(IndexSetConfig.DEFAULT_INDEX_TEMPLATE_TYPE)
        ).toTemplate(indexSet.getConfig(), indexWildcard);
    }

    public void deleteIndexTemplate(IndexSet indexSet) {
        final String templateName = indexSet.getConfig().indexTemplateName();

        final boolean result = indicesAdapter.deleteIndexTemplate(templateName);
        if (result) {
            LOG.info("Successfully deleted index template {}", templateName);
        }
    }

    public boolean create(String indexName, IndexSet indexSet) {
        final IndexSettings indexSettings = IndexSettings.create(
                indexSet.getConfig().shards(),
                indexSet.getConfig().replicas()
        );

        final IndexSetConfig indexSetConfig = indexSet.getConfig();
        final String templateName = indexSetConfig.indexTemplateName();
        final Map<String, Object> template = buildTemplate(indexSet, indexSetConfig);

        try {
            // Make sure our index template exists before creating an index!
            indicesAdapter.ensureIndexTemplate(templateName, template);
            indicesAdapter.create(indexName, indexSettings, templateName, template);
        } catch (Exception e) {
            LOG.warn("Couldn't create index {}. Error: {}", indexName, e.getMessage(), e);
            auditEventSender.failure(AuditActor.system(nodeId), ES_INDEX_CREATE, ImmutableMap.of("indexName", indexName));
            return false;
        }

        auditEventSender.success(AuditActor.system(nodeId), ES_INDEX_CREATE, ImmutableMap.of("indexName", indexName));
        return true;
    }

    private Map<String, Object> buildTemplate(IndexSet indexSet, IndexSetConfig indexSetConfig) {
        final IndexSetConfig.TemplateType templateType = indexSetConfig.indexTemplateType().orElse(IndexSetConfig.DEFAULT_INDEX_TEMPLATE_TYPE);
        final IndexMappingTemplate indexMapping = indexMappingFactory.createIndexMapping(templateType);

        return indexMapping.toTemplate(indexSetConfig, indexSet.getIndexWildcard(), -1);
    }

    public Map<String, Set<String>> getAllMessageFieldsForIndices(final String[] writeIndexWildcards) {
        return indicesAdapter.fieldsInIndices(writeIndexWildcards);
    }

    public Set<String> getAllMessageFields(final String[] writeIndexWildcards) {
        final Map<String, Set<String>> fieldsForIndices = getAllMessageFieldsForIndices(writeIndexWildcards);
        final ImmutableSet.Builder<String> result = ImmutableSet.builder();
        for (Set<String> fields : fieldsForIndices.values()) {
            result.addAll(fields);
        }
        return result.build();
    }

    public void setReadOnly(String index) {
        indicesAdapter.setReadOnly(index);
    }

    public void flush(String index) {
        indicesAdapter.flush(index);
    }

    public void reopenIndex(String index) {
        // Mark this index as re-opened. It will never be touched by retention.
        markIndexReopened(index);

        // Open index.
        openIndex(index);
    }

    public void markIndexReopened(String index) {
        indicesAdapter.markIndexReopened(index);
    }

    private void openIndex(String index) {
        indicesAdapter.openIndex(index);
        //noinspection UnstableApiUsage
        eventBus.post(IndicesReopenedEvent.create(index));
    }

    public boolean isReopened(String indexName) {
        final Optional<String> aliasTarget = aliasTarget(indexName + REOPENED_ALIAS_SUFFIX);

        return aliasTarget.map(target -> target.equals(indexName)).orElse(false);
    }

    public Map<String, Boolean> areReopened(Collection<String> indices) {
        return indices.stream().collect(Collectors.toMap(Function.identity(), this::isReopened));
    }

    public Set<String> getClosedIndices(final Collection<String> indices) {
        return indicesAdapter.closedIndices(indices);
    }

    public Set<String> getClosedIndices(final IndexSet indexSet) {
        return getClosedIndices(Collections.singleton(indexSet.getIndexWildcard()));
    }

    public Set<String> getIndices(final IndexSet indexSet, final String... statusFilter) {
        final String indexWildcard = indexSet.getIndexWildcard();
        final List<String> status = Arrays.asList(statusFilter);
        return indicesAdapter.indices(indexWildcard, status, indexSet.getConfig().id());
    }

    public boolean isOpen(final String indexName) {
        return indicesAdapter.isOpen(indexName);
    }

    public boolean isClosed(final String indexName) {
        return indicesAdapter.isClosed(indexName);
    }

    public Set<String> getReopenedIndices(final Collection<String> indices) {
        return indices.stream()
            .filter(this::isReopened)
            .collect(Collectors.toSet());
    }

    public Set<String> getReopenedIndices(final IndexSet indexSet) {
        return getReopenedIndices(Collections.singleton(indexSet.getIndexWildcard()));
    }

    public Optional<IndexStatistics> getIndexStats(String index) {
        return indicesAdapter.getIndexStats(index);
    }

    public Optional<Long> getStoreSizeInBytes(String index) {
        return indicesAdapter.storeSizeInBytes(index);
    }

    public Set<IndexStatistics> getIndicesStats(final IndexSet indexSet) {
        return getIndicesStats(Collections.singleton(indexSet.getIndexWildcard()));
    }

    public Set<IndexStatistics> getIndicesStats(final Collection<String> indices) {
        return indicesAdapter.indicesStats(indices);
    }

    public void cycleAlias(String aliasName, String targetIndex) {
        indicesAdapter.cycleAlias(aliasName, targetIndex);
    }

    public void cycleAlias(String aliasName, String targetIndex, String oldIndex) {
        indicesAdapter.cycleAlias(aliasName, targetIndex, oldIndex);
    }

    public void removeAliases(String alias, Set<String> indices) {
        indicesAdapter.removeAliases(indices, alias);
    }

    public void optimizeIndex(String index, int maxNumSegments, Duration timeout) {
        indicesAdapter.optimizeIndex(index, maxNumSegments, timeout);
    }

    public HealthStatus waitForRecovery(String index) {
        LOG.debug("Waiting until index health status of index {} is healthy", index);
        return indicesAdapter.waitForRecovery(index);
    }

    public Optional<DateTime> indexCreationDate(String index) {
        return indicesAdapter.indexCreationDate(index);
    }

    public IndexRangeStats indexRangeStatsOfIndex(String index) {
        return indicesAdapter.indexRangeStatsOfIndex(index);
    }
}
