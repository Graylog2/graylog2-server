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
package org.graylog.storage.opensearch3;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.joschi.jadconfig.util.Duration;
import com.google.common.collect.ImmutableMap;
import jakarta.inject.Inject;
import org.apache.commons.lang3.EnumUtils;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.cluster.health.ClusterHealthRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.cluster.health.ClusterHealthResponse;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.indices.flush.FlushRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.indices.forcemerge.ForceMergeRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.indices.open.OpenIndexRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.indices.refresh.RefreshRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchResponse;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchType;
import org.graylog.shaded.opensearch2.org.opensearch.action.support.IndicesOptions;
import org.graylog.shaded.opensearch2.org.opensearch.client.GetAliasesResponse;
import org.graylog.shaded.opensearch2.org.opensearch.client.Requests;
import org.graylog.shaded.opensearch2.org.opensearch.client.indices.CloseIndexRequest;
import org.graylog.shaded.opensearch2.org.opensearch.cluster.metadata.AliasMetadata;
import org.graylog.shaded.opensearch2.org.opensearch.common.settings.Settings;
import org.graylog.shaded.opensearch2.org.opensearch.common.unit.TimeValue;
import org.graylog.shaded.opensearch2.org.opensearch.index.query.QueryBuilders;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.filter.Filter;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.terms.Terms;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.Max;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.Min;
import org.graylog.shaded.opensearch2.org.opensearch.search.builder.SearchSourceBuilder;
import org.graylog.storage.opensearch3.blocks.BlockSettingsParser;
import org.graylog.storage.opensearch3.cluster.ClusterStateApi;
import org.graylog.storage.opensearch3.stats.ClusterStatsApi;
import org.graylog.storage.opensearch3.stats.IndexStatisticsBuilder;
import org.graylog.storage.opensearch3.stats.StatsApi;
import org.graylog2.datatiering.WarmIndexInfo;
import org.graylog2.indexer.IndexNotFoundException;
import org.graylog2.indexer.indices.HealthStatus;
import org.graylog2.indexer.indices.IndexMoveResult;
import org.graylog2.indexer.indices.IndexSettings;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.indices.IndicesAdapter;
import org.graylog2.indexer.indices.ShardsInfo;
import org.graylog2.indexer.indices.Template;
import org.graylog2.indexer.indices.blocks.IndicesBlockStatus;
import org.graylog2.indexer.indices.stats.IndexStatistics;
import org.graylog2.indexer.searches.IndexRangeStats;
import org.graylog2.plugin.Message;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetStats;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.ExpandWildcard;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch.cat.IndicesRequest;
import org.opensearch.client.opensearch.cat.IndicesResponse;
import org.opensearch.client.opensearch.cat.OpenSearchCatClient;
import org.opensearch.client.opensearch.cat.indices.IndicesRecord;
import org.opensearch.client.opensearch.core.ReindexResponse;
import org.opensearch.client.opensearch.core.reindex.Destination;
import org.opensearch.client.opensearch.core.reindex.Source;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.GetAliasRequest;
import org.opensearch.client.opensearch.indices.GetAliasResponse;
import org.opensearch.client.opensearch.indices.GetIndexRequest;
import org.opensearch.client.opensearch.indices.GetIndexResponse;
import org.opensearch.client.opensearch.indices.GetIndicesSettingsResponse;
import org.opensearch.client.opensearch.indices.GetMappingResponse;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;
import org.opensearch.client.opensearch.indices.PutMappingRequest;
import org.opensearch.client.opensearch.indices.get_mapping.IndexMappingRecord;
import org.opensearch.client.opensearch.indices.stats.IndicesStats;
import org.opensearch.client.opensearch.indices.update_aliases.AddAction;
import org.opensearch.client.opensearch.indices.update_aliases.RemoveAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.ObjectUtils.getIfNull;
import static org.graylog.storage.opensearch3.OpenSearchClient.withTimeout;

public class IndicesAdapterOS implements IndicesAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(IndicesAdapterOS.class);
    private final OfficialOpensearchClient c;
    private final OpenSearchClient client;
    private final StatsApi statsApi;
    private final ClusterStatsApi clusterStatsApi;
    private final ClusterStateApi clusterStateApi;
    private final IndexTemplateAdapter indexTemplateAdapter;
    private final IndexStatisticsBuilder indexStatisticsBuilder;
    private final ObjectMapper objectMapper;

    private final org.opensearch.client.opensearch.OpenSearchClient openSearchClient;
    private final OpenSearchIndicesClient indicesClient;
    private final OpenSearchCatClient catClient;

    // this is the maximum amount of bytes that the index list is supposed to fill in a request,
    // it assumes that these don't need url encoding. If we exceed the maximum, we request settings for all indices
    // and filter after wards
    private final int MAX_INDICES_URL_LENGTH = 3000;

    @Inject
    public IndicesAdapterOS(OpenSearchClient client,
                            OfficialOpensearchClient c,
                            StatsApi statsApi,
                            ClusterStatsApi clusterStatsApi,
                            ClusterStateApi clusterStateApi,
                            IndexTemplateAdapter indexTemplateAdapter,
                            IndexStatisticsBuilder indexStatisticsBuilder,
                            ObjectMapper objectMapper) {
        this.client = client;
        this.c = c;
        this.statsApi = statsApi;
        this.clusterStatsApi = clusterStatsApi;
        this.clusterStateApi = clusterStateApi;
        this.indexTemplateAdapter = indexTemplateAdapter;
        this.indexStatisticsBuilder = indexStatisticsBuilder;
        this.objectMapper = objectMapper;
        this.openSearchClient = c.sync();
        this.indicesClient = openSearchClient.indices();
        this.catClient = openSearchClient.cat();
    }

    @Override
    public void move(String source, String target, Consumer<IndexMoveResult> resultCallback) {
        ReindexResponse result = c.execute(() -> openSearchClient.reindex(
                        org.opensearch.client.opensearch.core.ReindexRequest.builder()
                                .source(Source.builder().index(source).build())
                                .dest(Destination.builder().index(target).build())
                                .build()),
                "Error moving index " + source + " to " + target);

        final IndexMoveResult indexMoveResult = IndexMoveResult.create(
                Math.toIntExact(getIfNull(result.total(), 0L)),
                getIfNull(result.took(), 0L),
                !result.failures().isEmpty()
        );
        resultCallback.accept(indexMoveResult);
    }

    @Override
    public void delete(String index) {
        c.execute(() -> indicesClient.delete(
                        org.opensearch.client.opensearch.indices.DeleteIndexRequest.builder()
                                .index(index)
                                .build()),
                "Error removing index " + index);
    }

    @Override
    public Set<String> resolveAlias(String alias) {
        if (!aliasExists(alias)) { // needed to ensure same behavior as old client
            return Collections.emptySet();
        }
        GetAliasResponse result = c.execute(() -> indicesClient.getAlias(GetAliasRequest.builder()
                .name(alias)
                .build()), "Error resolving alias " + alias);
        return result.result().keySet();
    }

    @Override
    public void create(String index, IndexSettings indexSettings) {
        executeCreateIndexRequest(index, createIndexRequest(index, indexSettings, null));
    }

    @Override
    public void create(String index, IndexSettings indexSettings, @Nullable Map<String, Object> mapping) {
        executeCreateIndexRequest(index, createIndexRequest(index, indexSettings, mapping));
    }

    private CreateIndexRequest createIndexRequest(String index,
                                                  IndexSettings indexSettings,
                                                  @Nullable Map<String, Object> mapping) {

        CreateIndexRequest.Builder builder = new CreateIndexRequest.Builder()
                .index(index)
                .settings(createIndexSettings(indexSettings));
        if (mapping != null) {
            // TODO: This sucks
            try {
                builder.mappings(typeMappingFromMap(mapping));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return builder.build();
    }

    private org.opensearch.client.opensearch.indices.IndexSettings createIndexSettings(IndexSettings indexSettings) {

        Map<String, JsonData> jsonSettings = indexSettings.map().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> JsonData.of(entry.getValue())
                ));

        org.opensearch.client.opensearch.indices.IndexSettings.Builder builder = org.opensearch.client.opensearch.indices.IndexSettings.builder();
        builder.customSettings(jsonSettings);
        return builder.build();
    }


    private void executeCreateIndexRequest(String index, CreateIndexRequest request) {
        c.execute(() -> indicesClient.create(request), "Unable to create index " + index);
    }

    @Override
    public void updateIndexMapping(@Nonnull String indexName,
                                   @Nonnull String mappingType,
                                   @Nonnull Map<String, Object> mapping) {

        // TODO: This sucks
        PutMappingRequest request = PutMappingRequest.of(b -> b);

        c.execute(() -> indicesClient.putMapping(request),
                "Unable to update index mapping " + indexName);
    }

    @Override
    public Map<String, Object> getIndexMapping(@Nonnull String index) {
        return c.execute(() -> {
            GetMappingResponse response = indicesClient.getMapping(r -> r.index(index)
                    .allowNoIndices(true)
                    .ignoreUnavailable(true)
                    .expandWildcards(ExpandWildcard.All));
            IndexMappingRecord indexMappingRecord = response.get(index);
            return typeMappingToMap(indexMappingRecord.mappings());
        }, "Couldn't read mapping of index " + index);
    }

    private Map<String, Object> typeMappingToMap(TypeMapping mapping) throws JsonProcessingException {
        return objectMapper.readValue(mapping.toJsonString(), new TypeReference<>() {});
    }

    private TypeMapping typeMappingFromMap(Map<String, Object> mapping) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(mapping);
        return objectMapper.readValue(json, TypeMapping.class);
    }

    @Override
    public Map<String, Object> getStructuredIndexSettings(@Nonnull String index) {

        return c.execute(() -> {
            GetIndicesSettingsResponse result = indicesClient.getSettings(b -> b.index(index)
                    .ignoreUnavailable(true)
                    .allowNoIndices(true)
                    .expandWildcards(ExpandWildcard.Open)
                    .flatSettings(true)
            );
            return objectMapper.readValue(result.toJsonString(), new TypeReference<>() {});
        }, "Couldn't read settings of index " + index);

    }


    @Override
    public void updateIndexMetaData(@Nonnull String index, @Nonnull Map<String, Object> metadata, boolean mergeExisting) {
        Map<String, Object> metaUpdate = new HashMap<>();
        if (mergeExisting) {
            final Map<String, Object> oldMetaData = getIndexMetaData(index);
            metaUpdate.putAll(oldMetaData);
        }
        metaUpdate.putAll(metadata);
        updateIndexMapping(index, "ignored", Map.of("_meta", metaUpdate));
    }

    @Override
    public Map<String, Object> getIndexMetaData(@Nonnull String index) {
        final Object metaData = getIndexMapping(index).get("_meta");
        //noinspection rawtypes
        if (metaData instanceof Map map) {
            //noinspection unchecked
            return map;
        }
        return Map.of();
    }

    @Override
    public boolean ensureIndexTemplate(String templateName, Template template) {
        return indexTemplateAdapter.ensureIndexTemplate(templateName, template);
    }

    @Override
    public boolean indexTemplateExists(String templateName) {
        return indexTemplateAdapter.indexTemplateExists(templateName);
    }

    @Override
    public boolean deleteIndexTemplate(String templateName) {
        return indexTemplateAdapter.deleteIndexTemplate(templateName);
    }

    @Override
    public Optional<DateTime> indexCreationDate(String index) {
        final GetSettingsRequest request = new GetSettingsRequest()
                .indices(index)
                .indicesOptions(IndicesOptions.LENIENT_EXPAND_OPEN);

        final GetSettingsResponse result = client.execute((c, requestOptions) -> c.indices().getSettings(request, requestOptions),
                "Couldn't read settings of index " + index);

        final Optional<String> creationDate = Optional.ofNullable(result.getIndexToSettings().get(index))
                .map(indexSettings -> indexSettings.get("index.creation_date"));

        return creationDate
                .map(Long::valueOf)
                .map(instant -> new DateTime(instant, DateTimeZone.UTC));
    }

    @Override
    public Optional<DateTime> indexClosingDate(String index) {
        final Map<String, Object> indexMetaData = getIndexMetaData(index);
        return Optional.ofNullable(indexMetaData.get("closing_date")).filter(Long.class::isInstance).map(Long.class::cast)
                .map(instant -> new DateTime(instant, DateTimeZone.UTC));
    }

    @Override
    public void openIndex(String index) {
        final OpenIndexRequest request = new OpenIndexRequest(index);

        client.execute((c, requestOptions) -> c.indices().open(request, requestOptions),
                "Unable to open index " + index);
    }

    @Override
    public void setReadOnly(String index) {
        // https://www.elastic.co/guide/en/elasticsearch/reference/7.8/indices-update-settings.html
        // https://www.elastic.co/guide/en/elasticsearch/reference/7.10/index-modules-blocks.html
        final Map<String, Object> settings = ImmutableMap.of(
                "index", ImmutableMap.of("blocks",
                        ImmutableMap.of(
                                "write", true, // Block writing.
                                "read", false, // Allow reading.
                                "metadata", false) // Allow getting metadata.
                )
        );

        final UpdateSettingsRequest request = new UpdateSettingsRequest(index)
                .settings(settings);
        client.execute((c, requestOptions) -> c.indices().putSettings(request, requestOptions),
                "Couldn't set index " + index + " to read-only");
    }

    @Override
    public void flush(String index) {
        final FlushRequest request = new FlushRequest(index);

        client.execute((c, requestOptions) -> c.indices().flush(request, requestOptions),
                "Unable to flush index " + index);
    }

    @Override
    public void markIndexReopened(String index) {
        final String aliasName = index + Indices.REOPENED_ALIAS_SUFFIX;
        final IndicesAliasesRequest indicesAliasesRequest = new IndicesAliasesRequest();
        final IndicesAliasesRequest.AliasActions aliasAction = new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.ADD)
                .index(index)
                .alias(aliasName);
        indicesAliasesRequest.addAliasAction(aliasAction);

        client.execute((c, requestOptions) -> c.indices().updateAliases(indicesAliasesRequest, requestOptions),
                "Couldn't create reopened alias for index " + index);
    }

    @Override
    public void removeAlias(String index, String alias) {
        removeAliases(Set.of(index), alias);
    }

    @Override
    public void close(String index) {
        final CloseIndexRequest request = new CloseIndexRequest(index);

        client.execute((c, requestOptions) -> c.indices().close(request, requestOptions),
                "Unable to close index " + index);
    }

    @Override
    public long numberOfMessages(final String index) {
        return statsApi.numberOfMessagesInIndex(index);
    }

    private GetSettingsResponse settingsFor(String indexOrAlias) {
        final GetSettingsRequest request = new GetSettingsRequest().indices(indexOrAlias)
                .indicesOptions(IndicesOptions.LENIENT_EXPAND_OPEN_CLOSED);
        return client.execute((c, requestOptions) -> c.indices().getSettings(request, requestOptions),
                "Unable to retrieve settings for index/alias " + indexOrAlias);
    }

    @Override
    public Map<String, Set<String>> aliases(String indexPattern) {
        final GetAliasesRequest request = new GetAliasesRequest()
                .indices(indexPattern)
                .indicesOptions(IndicesOptions.fromOptions(false, false, true, false));
        final GetAliasesResponse result = client.execute((c, requestOptions) -> c.indices().getAlias(request, requestOptions),
                "Couldn't collect aliases for index pattern " + indexPattern);
        return result.getAliases()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream().map(AliasMetadata::alias).collect(Collectors.toSet())
                ));
    }

    @Override
    public Map<String, Set<String>> fieldsInIndices(String[] writeIndexWildcards) {
        final List<String> indexWildCards = Arrays.asList(writeIndexWildcards);
        return clusterStateApi.fields(indexWildCards);
    }

    @Override
    public Set<String> closedIndices(Collection<String> indices) {
        return c.execute(() -> {
            GetIndexResponse result = indicesClient.get(GetIndexRequest.of(b -> b
                    .index(indices.stream().toList())
                    .expandWildcards(ExpandWildcard.Closed)
                    .ignoreUnavailable(true)));
            return result.result().keySet();
        }, "Unable to retrieve list of closed indices for " + indices);
    }

    @Override
    public Set<IndexStatistics> indicesStats(final Collection<String> indices) {
        return statsApi.indicesStatsWithShardLevel(indices).entrySet()
                .stream()
                .map(entry -> {
                    final String index = entry.getKey();
                    final IndicesStats indexStats = entry.getValue();
                    if (indexStats != null) {
                        return indexStatisticsBuilder.build(index, indexStats);
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<IndexStatistics> getIndexStats(final String index) {
        final IndicesStats indicesStats = statsApi.indexStatsWithShardLevel(index);
        return indicesStats == null
                ? Optional.empty()
                : Optional.of(indexStatisticsBuilder.build(index, indicesStats));
    }

    @Override
    public JsonNode getIndexStats(Collection<String> indices) {

        try {
            final Map<String, IndicesStats> stringIndicesStatsMap = statsApi.indicesStatsWithDocsAndStore(indices);
            ObjectNode node = objectMapper.createObjectNode();
            for (Map.Entry<String, IndicesStats> entry : stringIndicesStatsMap.entrySet()) {
                final JsonNode statsNode = objectMapper.readTree(entry.getValue().toJsonString());
                node.set(entry.getKey(), statsNode);
            }
            return node;
        } catch (JsonProcessingException e) {
            LOG.error("Unable to convert indices stats to JSON", e);
            return null;
        }

    }

    @Override
    public IndexSetStats getIndexSetStats() {
        return clusterStatsApi.clusterStats();
    }

    @Override
    public List<ShardsInfo> getShardsInfo(String indexName) {
        return c.execute(() -> catClient.shards().valueBody().stream()
                .map(shardsRecord -> new ShardsInfo(
                        shardsRecord.index(), Integer.parseInt(getIfNull(shardsRecord.shard(), "0")),
                        ShardsInfo.ShardType.fromString(getIfNull(shardsRecord.prirep(), "UNKNOWN")),
                        EnumUtils.getEnumIgnoreCase(ShardsInfo.State.class, shardsRecord.state(), ShardsInfo.State.UNKNOWN),
                        Long.parseLong(getIfNull(shardsRecord.docs(), "0")), shardsRecord.store(),
                        shardsRecord.ip(), shardsRecord.node())
                ).toList(), "Error getting shards information for " + indexName);
    }


    @Override
    public IndicesBlockStatus getIndicesBlocksStatus(final List<String> indices) {
        if (indices == null || indices.isEmpty()) {
            throw new IllegalArgumentException("Expecting list of indices with at least one index present.");
        }

        final GetSettingsRequest request = new GetSettingsRequest()
                .indicesOptions(IndicesOptions.fromOptions(false, true, true, true))
                .names("index.blocks.read", "index.blocks.write", "index.blocks.metadata", "index.blocks.read_only", "index.blocks.read_only_allow_delete");

        final var maxLengthExceeded = String.join(",", indices).length() > MAX_INDICES_URL_LENGTH;
        final GetSettingsRequest getSettingsRequest = maxLengthExceeded ? request : request.indices(indices.toArray(new String[]{}));

        return client.execute((c, requestOptions) -> {
            final GetSettingsResponse settingsResponse = c.indices().getSettings(getSettingsRequest, requestOptions);
            return BlockSettingsParser.parseBlockSettings(settingsResponse, maxLengthExceeded ? Optional.of(indices) : Optional.empty());
        });
    }

    @Override
    public boolean exists(String index) {
        final GetSettingsResponse result = settingsFor(index);
        return result.getIndexToSettings().size() == 1 && result.getIndexToSettings().containsKey(index);
    }

    @Override
    public boolean aliasExists(String alias) {
        return c.execute(() -> indicesClient.existsAlias(r -> r.name(alias)).value(),
                "Error trying to check if alias exists: " + alias);
    }

    @Override
    public Set<String> indices(String indexWildcard, List<String> status, String indexSetId) {
        return c.execute(() -> {
            GetIndexResponse result = indicesClient.get(GetIndexRequest.of(b -> b
                    .index(indexWildcard)
                    .expandWildcards(status.stream().map(ExpandWildcard::valueOf).collect(Collectors.toList()))
                    .ignoreUnavailable(true)));
            return result.result().keySet();
        }, "Couldn't get index list for index set <" + indexSetId + ">");
    }

    @Override
    public Optional<Long> storeSizeInBytes(String index) {
        return statsApi.storeSizes(index);
    }

    @Override
    public void cycleAlias(String aliasName, String targetIndex) {
        c.execute(() -> indicesClient.updateAliases(request ->
                request.actions(action ->
                        action.add(addAction ->
                                addAction.index(targetIndex).alias(aliasName)
                        )
                )
        ), "Couldn't point alias " + aliasName + " to index " + targetIndex);
    }

    @Override
    public void cycleAlias(String aliasName, String targetIndex, String oldIndex) {
        c.execute(() -> indicesClient.updateAliases(request ->
                request.actions(
                        new RemoveAction.Builder().index(oldIndex).alias(aliasName).build().toAction(),
                        new AddAction.Builder().index(targetIndex).alias(aliasName).build().toAction()
                )
        ), "Couldn't switch alias " + aliasName + " from index " + oldIndex + " to index " + targetIndex);
    }

    @Override
    public void removeAliases(Set<String> indices, String alias) {
        c.execute(() -> indicesClient.updateAliases(request ->
                request.actions(action ->
                        action.remove(removeAction ->
                                removeAction.indices(indices.stream().toList()).alias(alias)
                        )
                )
        ), "Couldn't remove alias " + alias + " from indices " + indices);
    }

    @Override
    public void optimizeIndex(String index, int maxNumSegments, Duration timeout) {
        final ForceMergeRequest request = new ForceMergeRequest()
                .indices(index)
                .maxNumSegments(maxNumSegments)
                .flush(true);

        client.execute((c, requestOptions) -> c.indices().forcemerge(request, withTimeout(requestOptions, timeout)));
    }

    @Override
    public IndexRangeStats indexRangeStatsOfIndex(String index) {
        final FilterAggregationBuilder builder = AggregationBuilders.filter("agg", QueryBuilders.existsQuery(Message.FIELD_TIMESTAMP))
                .subAggregation(AggregationBuilders.min("ts_min").field(Message.FIELD_TIMESTAMP))
                .subAggregation(AggregationBuilders.max("ts_max").field(Message.FIELD_TIMESTAMP))
                .subAggregation(AggregationBuilders.terms("streams").size(Integer.MAX_VALUE).field(Message.FIELD_STREAMS));
        final SearchSourceBuilder query = SearchSourceBuilder.searchSource()
                .aggregation(builder)
                .size(0);

        final SearchRequest request = new SearchRequest()
                .source(query)
                .indices(index)
                .searchType(SearchType.DFS_QUERY_THEN_FETCH)
                .indicesOptions(IndicesOptions.lenientExpandOpen());

        final SearchResponse result = client.execute((c, requestOptions) -> c.search(request, requestOptions),
                "Couldn't build index range of index " + index);

        if (result.getTotalShards() == 0 || result.getAggregations() == null) {
            throw new IndexNotFoundException("Couldn't build index range of index " + index + " because it doesn't exist.");
        }
        final Filter f = result.getAggregations().get("agg");
        if (f == null) {
            throw new IndexNotFoundException("Couldn't build index range of index " + index + " because it doesn't exist.");
        } else if (f.getDocCount() == 0L) {
            LOG.debug("No documents with attribute \"timestamp\" found in index <{}>", index);
            return IndexRangeStats.EMPTY;
        }

        final Min minAgg = f.getAggregations().get("ts_min");
        final long minUnixTime = Double.valueOf(minAgg.getValue()).longValue();
        final DateTime min = new DateTime(minUnixTime, DateTimeZone.UTC);
        final Max maxAgg = f.getAggregations().get("ts_max");
        final long maxUnixTime = Double.valueOf(maxAgg.getValue()).longValue();
        final DateTime max = new DateTime(maxUnixTime, DateTimeZone.UTC);
        // make sure we return an empty list, so we can differentiate between old indices that don't have this information
        // and newer ones that simply have no streams.
        final Terms streams = f.getAggregations().get("streams");
        final List<String> streamIds = streams.getBuckets().stream()
                .map(MultiBucketsAggregation.Bucket::getKeyAsString)
                .collect(toList());

        return IndexRangeStats.create(min, max, streamIds);
    }

    @Override
    public HealthStatus waitForRecovery(String index) {
        return waitForRecovery(index, 30);
    }

    @Override
    public HealthStatus waitForRecovery(String index, int timeout) {
        final ClusterHealthRequest clusterHealthRequest = new ClusterHealthRequest(index).timeout(TimeValue.timeValueSeconds(timeout));
        clusterHealthRequest.waitForGreenStatus();

        final ClusterHealthResponse result = client.execute((c, requestOptions) -> c.cluster().health(clusterHealthRequest, requestOptions));
        return HealthStatus.fromString(result.getStatus().toString());
    }

    @Override
    public boolean isOpen(String index) {
        return indexHasState(index, State.Open);
    }

    @Override
    public boolean isClosed(String index) {
        return indexHasState(index, State.Closed);
    }

    @Override
    public void refresh(String... indices) {
        final RefreshRequest refreshRequest = Requests.refreshRequest(indices);
        client.execute((c, requestOptions) -> c.indices().refresh(refreshRequest, requestOptions));
    }

    private Boolean indexHasState(String index, State open) {
        return indexState(index)
                .map(state -> state.equals(open))
                .orElseThrow(() -> new IndexNotFoundException("Unable to determine state for absent index " + index));
    }

    private Optional<State> indexState(String index) {
        return c.execute(() -> {
            IndicesResponse indices = catClient.indices(IndicesRequest.of(b -> b.index(index).headers("index", "status")));
            return indices.valueBody().stream()
                    .map(IndicesRecord::status)
                    .filter(Objects::nonNull)
                    .map(State::parse)
                    .findFirst();
        }, "Unable to retrieve index stats for " + index);
    }

    enum State {
        Open,
        Closed;

        static State parse(String state) {
            switch (state.toLowerCase(Locale.ENGLISH)) {
                case "open":
                    return Open;
                case "close":
                    return Closed;
            }

            throw new IllegalStateException("Unable to parse invalid index state: " + state);
        }
    }

    @Override
    public String getIndexId(String index) {
        final GetSettingsRequest request = new GetSettingsRequest().indices(index)
                .indicesOptions(IndicesOptions.LENIENT_EXPAND_OPEN_CLOSED);
        final GetSettingsResponse response = client.execute((c, requestOptions) -> c.indices().getSettings(request, requestOptions),
                "Unable to retrieve settings for index/alias " + index);
        return response.getSetting(index, "index.uuid");
    }

    @Override
    public Optional<WarmIndexInfo> getWarmIndexInfo(String index) {
        final GetSettingsResponse settingsResponse = client.execute((c, options) ->
                c.indices().getSettings(new GetSettingsRequest().indices(index), options));
        Map<String, Settings> indexToSettings = settingsResponse.getIndexToSettings();

        return Optional.ofNullable(indexToSettings.get(index))
                .filter(settings -> "remote_snapshot".equals(settings.get("index.store.type")))
                .map(settings -> mapIndexSettingsToSearchableSnapshot(index, settings));
    }

    private WarmIndexInfo mapIndexSettingsToSearchableSnapshot(String index, Settings settings) {
        String initialIndexName = settings.get("index.provided_name");
        Settings searchableSnapshotSettings = settings.getAsSettings("index.searchable_snapshot");
        String repository = searchableSnapshotSettings.get("repository");
        String snapshotName = searchableSnapshotSettings.get("snapshot_id.name");

        return new WarmIndexInfo(index, initialIndexName, repository, snapshotName);
    }
}
