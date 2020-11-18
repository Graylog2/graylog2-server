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
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.indices.flush.FlushRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.indices.forcemerge.ForceMergeRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.indices.open.OpenIndexRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.indices.template.delete.DeleteIndexTemplateRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchType;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.support.IndicesOptions;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.GetAliasesResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.CloseIndexRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.CreateIndexRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.DeleteAliasRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.PutIndexTemplateRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.cluster.metadata.AliasMetadata;
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
import org.graylog.storage.elasticsearch7.cat.CatApi;
import org.graylog.storage.elasticsearch7.cluster.ClusterStateApi;
import org.graylog.storage.elasticsearch7.stats.StatsApi;
import org.graylog2.indexer.IndexNotFoundException;
import org.graylog2.indexer.indices.HealthStatus;
import org.graylog2.indexer.indices.IndexMoveResult;
import org.graylog2.indexer.indices.IndexSettings;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.indices.IndicesAdapter;
import org.graylog2.indexer.indices.stats.IndexStatistics;
import org.graylog2.indexer.searches.IndexRangeStats;
import org.graylog2.plugin.Message;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
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

public class IndicesAdapterES7 implements IndicesAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(IndicesAdapterES7.class);
    private final ElasticsearchClient client;
    private final StatsApi statsApi;
    private final CatApi catApi;
    private final ClusterStateApi clusterStateApi;

    @Inject
    public IndicesAdapterES7(ElasticsearchClient client,
                             StatsApi statsApi,
                             CatApi catApi,
                             ClusterStateApi clusterStateApi) {
        this.client = client;
        this.statsApi = statsApi;
        this.catApi = catApi;
        this.clusterStateApi = clusterStateApi;
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
    public void create(String index, IndexSettings indexSettings, String templateName, Map<String, Object> template) {
        final Map<String, Object> settings = new HashMap<>();
        settings.put("number_of_shards", indexSettings.shards());
        settings.put("number_of_replicas", indexSettings.replicas());

        final CreateIndexRequest request = new CreateIndexRequest(index)
                .settings(settings);

        client.execute((c, requestOptions) -> c.indices().create(request, requestOptions),
                "Unable to create index " + index);
    }

    @Override
    public boolean ensureIndexTemplate(String templateName, Map<String, Object> template) {
        final PutIndexTemplateRequest request = new PutIndexTemplateRequest(templateName)
                .source(template);

        final AcknowledgedResponse result = client.execute((c, requestOptions) -> c.indices().putTemplate(request, requestOptions),
                "Unable to create index template " + templateName);

        return result.isAcknowledged();
    }

    @Override
    public Optional<DateTime> indexCreationDate(String index) {
        final GetSettingsRequest request = new GetSettingsRequest()
                .indices(index)
                .indicesOptions(IndicesOptions.fromOptions(true, true, true, false));

        final GetSettingsResponse result = client.execute((c, requestOptions) -> c.indices().getSettings(request, requestOptions),
                "Couldn't read settings of index " + index);

        final Optional<String> creationDate = Optional.ofNullable(result.getIndexToSettings().get(index))
                .map(indexSettings -> indexSettings.get("index.creation_date"));

        return creationDate
                .map(Long::valueOf)
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
        return result.path("primaries").path("docs").path("count").asLong();
    }

    private GetSettingsResponse settingsFor(String indexOrAlias) {
        final GetSettingsRequest request = new GetSettingsRequest().indices(indexOrAlias)
                .indicesOptions(IndicesOptions.fromOptions(true, true, true, true));
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
    public boolean deleteIndexTemplate(String templateName) {
        final DeleteIndexTemplateRequest request = new DeleteIndexTemplateRequest(templateName);

        final AcknowledgedResponse result = client.execute((c, requestOptions) -> c.indices().deleteTemplate(request, requestOptions),
                "Unable to delete index template " + templateName);
        return result.isAcknowledged();
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
        final IndicesAliasesRequest.AliasActions aliasAction = new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.REMOVE_INDEX)
                .indices(indices.toArray(new String[0]))
                .alias(alias);
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

        client.execute((c, requestOptions) -> c.indices().forcemerge(request, requestOptions));
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
        final long minUnixTime = new Double(minAgg.getValue()).longValue();
        final DateTime min = new DateTime(minUnixTime, DateTimeZone.UTC);
        final Max maxAgg = f.getAggregations().get("ts_max");
        final long maxUnixTime = new Double(maxAgg.getValue()).longValue();
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
        final ClusterHealthRequest clusterHealthRequest = new ClusterHealthRequest(index);
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
                case "open": return Open;
                case "close": return Closed;
            }

            throw new IllegalStateException("Unable to parse invalid index state: " + state);
        }
    }
}
