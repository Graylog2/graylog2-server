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
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.validation.constraints.NotNull;
import org.graylog2.audit.AuditActor;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.datatiering.WarmIndexDeletedEvent;
import org.graylog2.datatiering.WarmIndexInfo;
import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.indexer.IgnoreIndexTemplate;
import org.graylog2.indexer.IndexMappingFactory;
import org.graylog2.indexer.IndexMappingTemplate;
import org.graylog2.indexer.IndexNotFoundException;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexTemplateNotFoundException;
import org.graylog2.indexer.MapperParsingException;
import org.graylog2.indexer.indexset.CustomFieldMappings;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetMappingTemplate;
import org.graylog2.indexer.indexset.profile.IndexFieldTypeProfile;
import org.graylog2.indexer.indexset.profile.IndexFieldTypeProfileService;
import org.graylog2.indexer.indices.blocks.IndicesBlockStatus;
import org.graylog2.indexer.indices.events.IndicesClosedEvent;
import org.graylog2.indexer.indices.events.IndicesDeletedEvent;
import org.graylog2.indexer.indices.events.IndicesReopenedEvent;
import org.graylog2.indexer.indices.stats.IndexStatistics;
import org.graylog2.indexer.searches.IndexRangeStats;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetStats;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import static org.graylog2.shared.utilities.StringUtils.f;

@Singleton
public class Indices {
    private static final Logger LOG = LoggerFactory.getLogger(Indices.class);
    public static final String REOPENED_ALIAS_SUFFIX = "_reopened";

    private final IndexMappingFactory indexMappingFactory;
    private final NodeId nodeId;
    private final AuditEventSender auditEventSender;
    private final EventBus eventBus;
    private final IndicesAdapter indicesAdapter;
    private final IndexFieldTypeProfileService profileService;

    @Inject
    public Indices(IndexMappingFactory indexMappingFactory,
                   NodeId nodeId,
                   AuditEventSender auditEventSender,
                   EventBus eventBus,
                   IndicesAdapter indicesAdapter,
                   IndexFieldTypeProfileService profileService) {
        this.indexMappingFactory = indexMappingFactory;
        this.nodeId = nodeId;
        this.auditEventSender = auditEventSender;
        this.eventBus = eventBus;
        this.indicesAdapter = indicesAdapter;
        this.profileService = profileService;
    }

    public IndicesBlockStatus getIndicesBlocksStatus(final List<String> indices) {
        if (indices == null || indices.isEmpty()) {
            return new IndicesBlockStatus();
        } else {
            return indicesAdapter.getIndicesBlocksStatus(indices);
        }
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
        Optional<WarmIndexInfo> snapshotInfoOptional = indicesAdapter.getWarmIndexInfo(indexName);
        indicesAdapter.delete(indexName);

        eventBus.post(IndicesDeletedEvent.create(indexName));
        snapshotInfoOptional.ifPresent(snapshotInfo -> eventBus.post(new WarmIndexDeletedEvent(snapshotInfo)));
    }

    public void close(String indexName) {
        indicesAdapter.getWarmIndexInfo(indexName).ifPresent(snapshotInfo -> {
            throw new UnsupportedOperationException("Close operation not available for warm index: " + snapshotInfo.currentIndexName());
        });

        if (isReopened(indexName)) {
            indicesAdapter.removeAlias(indexName, indexName + REOPENED_ALIAS_SUFFIX);
        }
        indicesAdapter.close(indexName);
        eventBus.post(IndicesClosedEvent.create(indexName));
    }

    public long numberOfMessages(String indexName) throws IndexNotFoundException {
        return indicesAdapter.numberOfMessages(indexName);
    }

    public IndexSetStats getIndexSetStats() {
        return indicesAdapter.getIndexSetStats();
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

    public Set<String> aliasTargets(String alias) {
        return indicesAdapter.resolveAlias(alias);
    }

    public void ensureIndexTemplate(IndexSet indexSet) {
        final IndexSetConfig indexSetConfig = indexSet.getConfig();
        final String templateName = indexSetConfig.indexTemplateName();
        try {
            var template = buildTemplate(indexSet, indexSetConfig);
            if (indicesAdapter.ensureIndexTemplate(templateName, template)) {
                LOG.info("Successfully ensured index template {}", templateName);
            } else {
                LOG.warn("Failed to create index template {}", templateName);
            }
        } catch (IgnoreIndexTemplate e) {
            LOG.warn(e.getMessage());
            if (e.isFailOnMissingTemplate() && !indicesAdapter.indexTemplateExists(templateName)) {
                throw new IndexTemplateNotFoundException(f("No index template with name '%s' (type - '%s') found in Elasticsearch",
                        templateName, indexSetConfig.indexTemplateType().orElse(null)));
            }
        }
    }

    public Template getIndexTemplate(IndexSet indexSet) {
        final IndexSetMappingTemplate indexSetMappingTemplate = getTemplateIndexSetConfig(indexSet, indexSet.getConfig(), profileService);
        return indexMappingFactory.createIndexMapping(indexSet.getConfig())
                .toTemplate(indexSetMappingTemplate);
    }

    Template buildTemplate(IndexSet indexSet, IndexSetConfig indexSetConfig) throws IgnoreIndexTemplate {
        final IndexSetMappingTemplate indexSetMappingTemplate = getTemplateIndexSetConfig(indexSet, indexSetConfig, profileService);
        return indexMappingFactory.createIndexMapping(indexSetConfig)
                .toTemplate(indexSetMappingTemplate, 0L);
    }

    public void deleteIndexTemplate(IndexSet indexSet) {
        final String templateName = indexSet.getConfig().indexTemplateName();

        final boolean result = indicesAdapter.deleteIndexTemplate(templateName);
        if (result) {
            LOG.info("Successfully deleted index template {}", templateName);
        }
    }

    public boolean create(String indexName, IndexSet indexSet) {
        return create(indexName, indexSet, null, null);
    }

    public boolean create(String indexName,
                          IndexSet indexSet,
                          @Nullable Map<String, Object> indexMapping,
                          @Nullable Map<String, Object> indexSettings) {
        try {
            // Make sure our index template exists before creating an index!
            ensureIndexTemplate(indexSet);
            Optional<IndexMappingTemplate> indexMappingTemplate = indexMapping(indexSet);
            IndexSettings settings = indexMappingTemplate
                    .map(t -> t.indexSettings(indexSet.getConfig(), indexSettings))
                    .orElse(IndexMappingTemplate.createIndexSettings(indexSet.getConfig()));

            Map<String, Object> mappings = indexMappingTemplate
                    .map(t -> t.indexMappings(indexSet.getConfig(), indexMapping))
                    .orElse(null);

            indicesAdapter.create(indexName, settings, mappings);
        } catch (Exception e) {
            if ((indexSettings != null || indexMapping != null) && e instanceof MapperParsingException) {
                LOG.info("Couldn't create index {}. Error: {}. Fall back to default settings/mappings and retry.", indexName, e.getMessage(), e);
                return create(indexName, indexSet, null, null);
            }
            LOG.warn("Couldn't create index {}. Error: {}", indexName, e.getMessage(), e);
            auditEventSender.failure(AuditActor.system(nodeId), ES_INDEX_CREATE, ImmutableMap.of("indexName", indexName));
            return false;
        }
        auditEventSender.success(AuditActor.system(nodeId), ES_INDEX_CREATE, ImmutableMap.of("indexName", indexName));
        return true;
    }

    private Optional<IndexMappingTemplate> indexMapping(IndexSet indexSet) {
        try {
            return Optional.of(indexMappingFactory.createIndexMapping(indexSet.getConfig()));
        } catch (IgnoreIndexTemplate e) {
            return Optional.empty();
        }
    }

    public IndexSetMappingTemplate getTemplateIndexSetConfig(
            final IndexSet indexSet,
            final IndexSetConfig indexSetConfig,
            final IndexFieldTypeProfileService profileService) {
        final String profileId = indexSetConfig.fieldTypeProfile();
        final CustomFieldMappings customFieldMappings = indexSetConfig.customFieldMappings();
        if (profileId != null && !profileId.isEmpty()) {
            final Optional<IndexFieldTypeProfile> fieldTypeProfile = profileService.get(profileId);
            if (fieldTypeProfile.isPresent() && !fieldTypeProfile.get().customFieldMappings().isEmpty()) {
                return new IndexSetMappingTemplate(indexSetConfig.indexAnalyzer(),
                        indexSet.getIndexWildcard(),
                        fieldTypeProfile.get().customFieldMappings().mergeWith(customFieldMappings));
            }
        }

        return new IndexSetMappingTemplate(indexSetConfig.indexAnalyzer(),
                indexSet.getIndexWildcard(),
                customFieldMappings);
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
        eventBus.post(IndicesReopenedEvent.create(index));
    }

    public boolean isReopened(String indexName) {
        final Set<String> aliasTarget = aliasTargets(indexName + REOPENED_ALIAS_SUFFIX);

        return !aliasTarget.isEmpty();
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

    public List<ShardsInfo> getShardsInfo(String indexName) {
        return indicesAdapter.getShardsInfo(indexName);
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

    HealthStatus waitForRecovery(String index, int timeout) {
        LOG.debug("Waiting until index health status of index {} is healthy", index);
        return indicesAdapter.waitForRecovery(index, timeout);
    }

    public HealthStatus waitForRecovery(String index) {
        LOG.debug("Waiting until index health status of index {} is healthy", index);
        return indicesAdapter.waitForRecovery(index);
    }

    public static <E extends Exception> void checkIfHealthy(HealthStatus healthStatus, Function<HealthStatus, E> errorMessageSupplier) throws E {
        if (healthStatus.equals(HealthStatus.Red)) {
            throw errorMessageSupplier.apply(healthStatus);
        }
    }

    public Optional<DateTime> indexCreationDate(String index) {
        return indicesAdapter.indexCreationDate(index);
    }

    public void setClosingDate(String index, DateTime closingDate) {
        indicesAdapter.updateIndexMetaData(index, Map.of("closing_date", closingDate.getMillis()), true);
    }

    public Optional<DateTime> indexClosingDate(String index) {
        return indicesAdapter.indexClosingDate(index);
    }

    public IndexRangeStats indexRangeStatsOfIndex(String index) {
        return indicesAdapter.indexRangeStatsOfIndex(index);
    }

    /**
     * Returns ES UUID of the index; null if it does not exist
     */
    public String getIndexId(String indexName) {
        return indicesAdapter.getIndexId(indexName);
    }

    public void refresh(String... indices) {
        indicesAdapter.refresh(indices);
    }

    public Map<String, Object> indexMapping(String index) {
        return indicesAdapter.getIndexMapping(index);
    }

    public Map<String, Object> indexSettings(String index) {
        return IndexSettingsHelper.getAsStructuredMap(indicesAdapter.getFlattenIndexSettings(index));
    }
}
