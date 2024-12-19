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
package org.graylog.storage.elasticsearch7;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.joschi.jadconfig.util.Duration;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import jakarta.inject.Inject;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.indices.flush.FlushRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.indices.forcemerge.ForceMergeRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.indices.open.OpenIndexRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchType;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.support.IndicesOptions;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.GetAliasesResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.Requests;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.CloseIndexRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.CreateIndexRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.DeleteAliasRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.GetMappingsRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.PutMappingRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.cluster.metadata.AliasMetadata;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.common.settings.Settings;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.common.unit.TimeValue;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.QueryBuilders;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.reindex.ReindexRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.Max;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.Min;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog.storage.elasticsearch7.blocks.BlockSettingsParser;
import org.graylog.storage.elasticsearch7.cat.CatApi;
import org.graylog.storage.elasticsearch7.cluster.ClusterStateApi;
import org.graylog.storage.elasticsearch7.stats.ClusterStatsApi;
import org.graylog.storage.elasticsearch7.stats.StatsApi;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.graylog.storage.elasticsearch7.ElasticsearchClient.withTimeout;

public class IndicesAdapterES7 implements IndicesAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(IndicesAdapterES7.class);
    private final ElasticsearchClient client;
    private final StatsApi statsApi;
    private final ClusterStatsApi clusterStatsApi;
    private final CatApi catApi;
    private final ClusterStateApi clusterStateApi;
    private final IndexTemplateAdapter indexTemplateAdapter;

    // this is the maximum amount of bytes that the index list is supposed to fill in a request,
    // it assumes that these don't need url encoding. If we exceed the maximum, we request settings for all indices
    // and filter after wards
    private final int MAX_INDICES_URL_LENGTH = 3000;

    @Inject
    public IndicesAdapterES7(ElasticsearchClient client,
                             StatsApi statsApi,
                             ClusterStatsApi clusterStatsApi,
                             CatApi catApi,
                             ClusterStateApi clusterStateApi,
                             IndexTemplateAdapter indexTemplateAdapter) {
        this.client = client;
        this.statsApi = statsApi;
        this.clusterStatsApi = clusterStatsApi;
        this.catApi = catApi;
        this.clusterStateApi = clusterStateApi;
        this.indexTemplateAdapter = indexTemplateAdapter;
    }

    @Override
    public void move(String source, String target, Consumer<IndexMoveResult> resultCallback) {
        final ReindexRequest request = new ReindexRequest();
        request.setSourceIndices(source);
        request.setDestIndex(target);

        final BulkByScrollResponse result = client.execute((c, requestOptions) -> c.reindex(request, requestOptions));

        final IndexMoveResult indexMoveResult = IndexMoveResult.create(
                Math.toIntExact(result.getTotal()),
                result.getTook().millis(),
                !result.getBulkFailures().isEmpty()
        );
        resultCallback.accept(indexMoveResult);
    }

    @Override
    public void delete(String index) {
        final DeleteIndexRequest request = new DeleteIndexRequest(index);

        client.execute((c, requestOptions) -> c.indices().delete(request, requestOptions));
    }

    @Override
    public Set<String> resolveAlias(String alias) {
        final GetAliasesRequest request = new GetAliasesRequest()
                .aliases(alias);
        final GetAliasesResponse result = client.execute((c, requestOptions) -> c.indices().getAlias(request, requestOptions));

        return result.getAliases().keySet();
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
        CreateIndexRequest request = new CreateIndexRequest(index).settings(indexSettings.map());
        if (mapping != null) {
            request = request.mapping(mapping);
        }
        return request;
    }

    private void executeCreateIndexRequest(String index, CreateIndexRequest request) {
        client.execute((c, requestOptions) -> c.indices().create(request, requestOptions),
                "Unable to create index " + index);
    }

    @Override
    public void updateIndexMapping(@Nonnull String indexName,
                                   @Nonnull String mappingType,
                                   @Nonnull Map<String, Object> mapping) {

        final PutMappingRequest request = new PutMappingRequest(indexName)
                .source(mapping);

        client.execute((c, requestOptions) -> c.indices().putMapping(request, requestOptions),
                "Unable to update index mapping " + indexName);
    }

    @Override
    public Map<String, Object> getIndexMapping(@Nonnull String index) {
        final GetMappingsRequest request = new GetMappingsRequest()
                .indices(index)
                .indicesOptions(IndicesOptions.LENIENT_EXPAND_OPEN);

        return client.execute((c, requestOptions) -> c.indices().getMapping(request, requestOptions).mappings().get(index).sourceAsMap(),
                "Couldn't read mapping of index " + index);
    }

    @Override
    public Map<String, Object> getFlattenIndexSettings(@Nonnull String index) {

        final GetSettingsRequest getSettingsRequest = new GetSettingsRequest()
                .indices(index)
                .indicesOptions(IndicesOptions.LENIENT_EXPAND_OPEN);

        return client.execute((c, requestOptions) -> {
            final GetSettingsResponse settingsResponse = c.indices().getSettings(getSettingsRequest, requestOptions);
            Settings settings = settingsResponse.getIndexToSettings().get(index);
            ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
            settings.keySet().forEach(k -> Optional.ofNullable(settings.get(k)).ifPresent(v -> builder.put(k, v)));
            return builder.build();
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
        final DeleteAliasRequest request = new DeleteAliasRequest(index, alias);

        client.execute((c, requestOptions) -> c.indices().deleteAlias(request, requestOptions),
                "Unable to remove alias " + alias + ", pointing to " + index);
    }

    @Override
    public void close(String index) {
        final CloseIndexRequest request = new CloseIndexRequest(index);

        client.execute((c, requestOptions) -> c.indices().close(request, requestOptions),
                "Unable to close index " + index);
    }

    @Override
    public long numberOfMessages(String index) {
        final JsonNode result = statsApi.indexStats(index);
        final JsonNode count = result.path("_all").path("primaries").path("docs").path("count");
        if (count.isMissingNode()) {
            throw new RuntimeException("Unable to extract count from response.");
        }
        return count.asLong();
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
        return catApi.indices(indices, Collections.singleton("close"),
                "Unable to retrieve list of closed indices for " + indices);
    }

    @Override
    public Set<IndexStatistics> indicesStats(Collection<String> indices) {
        final ImmutableSet.Builder<IndexStatistics> result = ImmutableSet.builder();

        final JsonNode allWithShardLevel = statsApi.indexStatsWithShardLevel(indices);
        final Iterator<Map.Entry<String, JsonNode>> fields = allWithShardLevel.fields();
        while (fields.hasNext()) {
            final Map.Entry<String, JsonNode> entry = fields.next();
            final String index = entry.getKey();
            final JsonNode indexStats = entry.getValue();
            if (indexStats.isObject()) {
                result.add(IndexStatistics.create(index, indexStats));
            }
        }

        return result.build();
    }

    @Override
    public Optional<IndexStatistics> getIndexStats(String index) {
        final JsonNode indexStats = statsApi.indexStatsWithShardLevel(index);
        return indexStats.isMissingNode()
                ? Optional.empty()
                : Optional.of(IndexStatistics.create(index, indexStats));
    }

    @Override
    public JsonNode getIndexStats(Collection<String> indices) {
        return statsApi.indexStatsWithDocsAndStore(indices);
    }

    @Override
    public IndexSetStats getIndexSetStats() {
        return clusterStatsApi.clusterStats();
    }

    @Override
    public List<ShardsInfo> getShardsInfo(String indexName) {
        return catApi.getShardsInfo(indexName);
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
        final GetAliasesRequest request = new GetAliasesRequest(alias);
        return client.execute((c, requestOptions) -> c.indices().existsAlias(request, requestOptions));
    }

    @Override
    public Set<String> indices(String indexWildcard, List<String> status, String indexSetId) {
        return catApi.indices(indexWildcard, status, "Couldn't get index list for index set <" + indexSetId + ">");
    }

    @Override
    public Optional<Long> storeSizeInBytes(String index) {
        return statsApi.storeSizes(index);
    }

    @Override
    public void cycleAlias(String aliasName, String targetIndex) {
        final IndicesAliasesRequest.AliasActions addAlias = new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.ADD)
                .index(targetIndex)
                .alias(aliasName);
        final IndicesAliasesRequest indicesAliasesRequest = new IndicesAliasesRequest()
                .addAliasAction(addAlias);

        client.execute((c, requestOptions) -> c.indices().updateAliases(indicesAliasesRequest, requestOptions),
                "Couldn't point alias " + aliasName + " to index " + targetIndex);
    }

    @Override
    public void cycleAlias(String aliasName, String targetIndex, String oldIndex) {
        final IndicesAliasesRequest.AliasActions addAlias = new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.ADD)
                .index(targetIndex)
                .alias(aliasName);
        final IndicesAliasesRequest.AliasActions removeAlias = new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.REMOVE)
                .index(oldIndex)
                .alias(aliasName);
        final IndicesAliasesRequest indicesAliasesRequest = new IndicesAliasesRequest()
                .addAliasAction(removeAlias)
                .addAliasAction(addAlias);

        client.execute((c, requestOptions) -> c.indices().updateAliases(indicesAliasesRequest, requestOptions),
                "Couldn't switch alias " + aliasName + " from index " + oldIndex + " to index " + targetIndex);
    }

    @Override
    public void removeAliases(Set<String> indices, String alias) {
        final IndicesAliasesRequest indicesAliasesRequest = new IndicesAliasesRequest();
        final IndicesAliasesRequest.AliasActions aliasAction = IndicesAliasesRequest.AliasActions.remove()
                .alias(alias)
                .indices(indices.toArray(new String[0]));
        indicesAliasesRequest.addAliasAction(aliasAction);

        client.execute((c, requestOptions) -> c.indices().updateAliases(indicesAliasesRequest, requestOptions),
                "Couldn't remove alias " + alias + " from indices " + indices);
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
        final Optional<String> result = catApi.indexState(index, "Unable to retrieve index stats for " + index);

        return result.map((State::parse));
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

    //Snapshots not supported for ES
    @Override
    public Optional<WarmIndexInfo> getWarmIndexInfo(String index) {
        return Optional.empty();
    }
}
