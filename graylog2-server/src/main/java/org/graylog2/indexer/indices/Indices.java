/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.indexer.indices;

import com.carrotsearch.hppc.cursors.ObjectObjectCursor;
import com.github.joschi.jadconfig.util.Duration;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.state.ClusterStateRequest;
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesResponse;
import org.elasticsearch.action.admin.indices.close.CloseIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.flush.FlushRequest;
import org.elasticsearch.action.admin.indices.forcemerge.ForceMergeRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.action.admin.indices.open.OpenIndexRequest;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.admin.indices.stats.IndexStats;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsRequest;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsResponse;
import org.elasticsearch.action.admin.indices.stats.ShardStats;
import org.elasticsearch.action.admin.indices.template.delete.DeleteIndexTemplateRequest;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.shard.DocsStats;
import org.elasticsearch.indices.IndexClosedException;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.max.Max;
import org.elasticsearch.search.aggregations.metrics.min.Min;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortParseElement;
import org.graylog2.audit.AuditActor;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.indexer.IndexMapping;
import org.graylog2.indexer.IndexNotFoundException;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.indexer.searches.IndexRangeStats;
import org.graylog2.plugin.system.NodeId;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.graylog2.audit.AuditEventTypes.ES_INDEX_CREATE;

@Singleton
public class Indices {
    private static final Logger LOG = LoggerFactory.getLogger(Indices.class);
    private static final String REOPENED_INDEX_SETTING = "graylog2_reopened";

    private final Client c;
    private final IndexMapping indexMapping;
    private final Messages messages;
    private final NodeId nodeId;
    private final AuditEventSender auditEventSender;

    @Inject
    public Indices(Client client, IndexMapping indexMapping, Messages messages, NodeId nodeId, AuditEventSender auditEventSender) {
        this.c = client;
        this.indexMapping = indexMapping;
        this.messages = messages;
        this.nodeId = nodeId;
        this.auditEventSender = auditEventSender;
    }

    public void move(String source, String target) {
        SearchResponse scrollResp = c.prepareSearch(source)
                .setScroll(TimeValue.timeValueSeconds(10L))
                .setQuery(matchAllQuery())
                .addSort(SortBuilders.fieldSort(SortParseElement.DOC_FIELD_NAME))
                .setSize(350)
                .execute()
                .actionGet();

        while (true) {
            scrollResp = c.prepareSearchScroll(scrollResp.getScrollId()).setScroll(new TimeValue(60000)).execute().actionGet();

            // No more hits.
            if (scrollResp.getHits().hits().length == 0) {
                break;
            }

            final BulkRequestBuilder request = c.prepareBulk();
            for (SearchHit hit : scrollResp.getHits()) {
                Map<String, Object> doc = hit.getSource();
                String id = (String) doc.remove("_id");

                request.add(messages.buildIndexRequest(target, doc, id));
            }

            request.setConsistencyLevel(WriteConsistencyLevel.ONE);

            if (request.numberOfActions() > 0) {
                BulkResponse response = c.bulk(request.request()).actionGet();

                LOG.info("Moving index <{}> to <{}>: Bulk indexed {} messages, took {} ms, failures: {}",
                        source,
                        target,
                        response.getItems().length,
                        response.getTookInMillis(),
                        response.hasFailures());

                if (response.hasFailures()) {
                    throw new RuntimeException("Failed to move a message. Check your indexer log.");
                }
            }
        }

    }

    public void delete(String indexName) {
        c.admin().indices().delete(new DeleteIndexRequest(indexName)).actionGet();
    }

    public void close(String indexName) {
        c.admin().indices().close(new CloseIndexRequest(indexName)).actionGet();
    }

    public long numberOfMessages(String indexName) throws IndexNotFoundException {
        final IndexStats index = indexStats(indexName);
        if (index == null) {
            throw new IndexNotFoundException("Couldn't find index " + indexName);
        }

        final DocsStats docsStats = index.getPrimaries().getDocs();

        return docsStats == null ? 0L : docsStats.getCount();
    }

    public Map<String, IndexStats> getAll(final IndexSet indexSet) {
        final IndicesStatsRequest request = c.admin().indices().prepareStats(indexSet.getIndexWildcard()).request();
        final IndicesStatsResponse response = c.admin().indices().stats(request).actionGet();

        if (response.getFailedShards() > 0) {
            LOG.warn("IndexStats response contains failed shards, response is incomplete: {}", (Object) response.getShardFailures());
        }
        return response.getIndices();
    }

    public Map<String, IndexStats> getAllDocCounts(final IndexSet indexSet) {
        final IndicesStatsRequest request = c.admin().indices().prepareStats(indexSet.getIndexWildcard()).setDocs(true).request();
        final IndicesStatsResponse response = c.admin().indices().stats(request).actionGet();

        return response.getIndices();
    }

    @Nullable
    public IndexStats indexStats(final String indexName) {
        final IndicesStatsRequest request = c.admin().indices().prepareStats(indexName).request();
        final IndicesStatsResponse response = c.admin().indices().stats(request).actionGet();

        return response.getIndex(indexName);
    }

    public boolean exists(String index) {
        ActionFuture<IndicesExistsResponse> existsFuture = c.admin().indices().exists(new IndicesExistsRequest(index));
        return existsFuture.actionGet().isExists();
    }

    public boolean aliasExists(String alias) {
        return c.admin().indices().aliasesExist(new GetAliasesRequest(alias)).actionGet().exists();
    }

    @NotNull
    public Map<String, Set<String>> getIndexNamesAndAliases(String indexPattern) {

        // only request indices matching the name or pattern in `indexPattern` and only get the alias names for each index,
        // not the settings or mappings
        final GetIndexRequestBuilder getIndexRequestBuilder = c.admin().indices().prepareGetIndex();
        getIndexRequestBuilder.addFeatures(GetIndexRequest.Feature.ALIASES);
        getIndexRequestBuilder.setIndices(indexPattern);

        final GetIndexResponse getIndexResponse = c.admin().indices().getIndex(getIndexRequestBuilder.request()).actionGet();

        final String[] indices = getIndexResponse.indices();
        final ImmutableOpenMap<String, List<AliasMetaData>> aliases = getIndexResponse.aliases();
        final Map<String, Set<String>> indexAliases = Maps.newHashMap();
        for (String index : indices) {
            final List<AliasMetaData> aliasMetaData = aliases.get(index);
            if (aliasMetaData == null) {
                indexAliases.put(index, Collections.emptySet());
            } else {
                indexAliases.put(index,
                                 aliasMetaData.stream()
                                         .map(AliasMetaData::alias)
                                         .collect(toSet()));
            }
        }

        return indexAliases;
    }

    @Nullable
    public String aliasTarget(String alias) throws TooManyAliasesException {
        final IndicesAdminClient indicesAdminClient = c.admin().indices();

        final GetAliasesRequest request = indicesAdminClient.prepareGetAliases(alias).request();
        final GetAliasesResponse response = indicesAdminClient.getAliases(request).actionGet();

        // The ES return value of this has an awkward format: The first key of the hash is the target index. Thanks.
        final ImmutableOpenMap<String, List<AliasMetaData>> aliases = response.getAliases();

        if (aliases.size() > 1) {
            throw new TooManyAliasesException(Sets.newHashSet(aliases.keysIt()));
        }
        return aliases.isEmpty() ? null : aliases.keysIt().next();
    }

    private void ensureIndexTemplate(IndexSet indexSet) {
        final Map<String, Object> template = indexMapping.messageTemplate(indexSet.getIndexWildcard(), indexSet.getConfig().indexAnalyzer());
        final PutIndexTemplateRequest itr = c.admin().indices().preparePutTemplate(indexSet.getConfig().indexTemplateName())
                .setOrder(-1) // Make sure templates with "order: 0" and higher are applied after our template!
                .setSource(template)
                .request();

        try {
            final boolean acknowledged = c.admin().indices().putTemplate(itr).actionGet().isAcknowledged();
            if (acknowledged) {
                LOG.info("Created Graylog index template \"{}\" in Elasticsearch.", indexSet.getConfig().indexTemplateName());
            }
        } catch (Exception e) {
            LOG.error("Unable to create the Graylog index template: " + indexSet.getConfig().indexTemplateName(), e);
        }
    }

    public void deleteIndexTemplate(IndexSet indexSet) {
        final String templateName = indexSet.getConfig().indexTemplateName();

        final DeleteIndexTemplateRequest deleteRequest = c.admin()
                .indices()
                .prepareDeleteTemplate(templateName)
                .request();

        try {
            final boolean acknowledged = c.admin()
                    .indices()
                    .deleteTemplate(deleteRequest)
                    .actionGet()
                    .isAcknowledged();
            if (acknowledged) {
                LOG.info("Deleted Graylog index template \"{}\" in Elasticsearch.", templateName);
            }
        } catch (Exception e) {
            LOG.error("Unable to delete the Graylog index template: " + templateName, e);
        }
    }

    public boolean create(String indexName, IndexSet indexSet) {
        return create(indexName, indexSet, Settings.EMPTY);
    }

    public boolean create(String indexName, IndexSet indexSet, Settings customSettings) {
        final Settings settings = Settings.builder()
                .put("number_of_shards", indexSet.getConfig().shards())
                .put("number_of_replicas", indexSet.getConfig().replicas())
                .put(customSettings)
                .build();

        // Make sure our index template exists before creating an index!
        ensureIndexTemplate(indexSet);

        final CreateIndexRequest cir = c.admin().indices().prepareCreate(indexName)
                .setSettings(settings)
                .request();

        final boolean acknowledged = c.admin().indices().create(cir).actionGet().isAcknowledged();
        if (acknowledged) {
            auditEventSender.success(AuditActor.system(nodeId), ES_INDEX_CREATE, ImmutableMap.of("indexName", indexName));
        } else {
            auditEventSender.failure(AuditActor.system(nodeId), ES_INDEX_CREATE, ImmutableMap.of("indexName", indexName));
        }
        return acknowledged;
    }

    public Map<String, Set<String>> getAllMessageFieldsForIndices(final String[] writeIndexWildcards) {
        final Map<String, Set<String>> fields = new HashMap<>();

        final ClusterStateRequest csr = new ClusterStateRequest().blocks(true).nodes(true).indices(writeIndexWildcards);
        final ClusterState cs = c.admin().cluster().state(csr).actionGet().getState();

        for (ObjectObjectCursor<String, IndexMetaData> m : cs.getMetaData().indices()) {
            try {
                MappingMetaData mmd = m.value.mapping(IndexMapping.TYPE_MESSAGE);
                if (mmd == null) {
                    // There is no mapping if there are no messages in the index.
                    continue;
                }
                @SuppressWarnings("unchecked")
                final Map<String, Object> mapping = (Map<String, Object>) mmd.getSourceAsMap().get("properties");

                if (mapping != null) {
                    fields.put(m.key, mapping.keySet());
                }
            } catch (Exception e) {
                LOG.error("Error while trying to get fields of <" + m.index + ">", e);
            }
        }

        return fields;
    }

    public Set<String> getAllMessageFields(final String[] writeIndexWildcards) {
        final Optional<Set<String>> result = getAllMessageFieldsForIndices(writeIndexWildcards)
            .values()
            .stream()
            .reduce((strings, strings2) -> ImmutableSet.<String>builder().addAll(strings).addAll(strings2).build());

        return result.orElse(Collections.emptySet());
    }

    public void setReadOnly(String index) {
        final Settings.Builder sb = Settings.builder()
                // https://www.elastic.co/guide/en/elasticsearch/reference/2.0/indices-update-settings.html
                .put("index.blocks.write", true) // Block writing.
                .put("index.blocks.read", false) // Allow reading.
                .put("index.blocks.metadata", false); // Allow getting metadata.

        final UpdateSettingsRequest request = c.admin().indices().prepareUpdateSettings(index)
                .setSettings(sb)
                .request();
        c.admin().indices().updateSettings(request).actionGet();
    }

    public void flush(String index) {
        FlushRequest flush = new FlushRequest(index);
        flush.force(true); // Just flushes. Even if it is not necessary.

        c.admin().indices().flush(new FlushRequest(index).force(true)).actionGet();
    }

    public Settings reopenIndexSettings() {
        return Settings.builder().put(REOPENED_INDEX_SETTING, true).build();
    }

    public void reopenIndex(String index) {
        // Mark this index as re-opened. It will never be touched by retention.
        UpdateSettingsRequest settings = new UpdateSettingsRequest(index);
        settings.settings(reopenIndexSettings());
        c.admin().indices().updateSettings(settings).actionGet();

        // Open index.
        c.admin().indices().open(new OpenIndexRequest(index)).actionGet();
    }

    public boolean isReopened(String indexName) {
        final ClusterState clusterState = c.admin().cluster().state(new ClusterStateRequest()).actionGet().getState();
        final IndexMetaData metaData = clusterState.getMetaData().getIndices().get(indexName);

        return checkForReopened(metaData);
    }

    public Map<String, Boolean> areReopened(Collection<String> indices) {
        final ClusterStateResponse clusterState = c.admin().cluster().prepareState().all().get();
        final ImmutableOpenMap<String, IndexMetaData> indicesMetaData = clusterState.getState().getMetaData().getIndices();
        return indices.stream().collect(
            Collectors.toMap(index -> index, index -> checkForReopened(indicesMetaData.get(index)))
        );
    }

    private boolean checkForReopened(@Nullable IndexMetaData metaData) {
        return Optional.ofNullable(metaData)
                .map(IndexMetaData::getSettings)
                .map(settings -> settings.getAsBoolean("index." + REOPENED_INDEX_SETTING, false))
                .orElse(false);
    }

    public Set<String> getClosedIndices(final IndexSet indexSet) {
        final Set<String> closedIndices = Sets.newHashSet();

        ClusterStateRequest csr = new ClusterStateRequest()
                .nodes(false)
                .routingTable(false)
                .blocks(false)
                .metaData(true);

        ClusterState state = c.admin().cluster().state(csr).actionGet().getState();

        UnmodifiableIterator<IndexMetaData> it = state.getMetaData().getIndices().valuesIt();

        while (it.hasNext()) {
            IndexMetaData indexMeta = it.next();
            // Only search in our indices.
            if (!indexMeta.getIndex().startsWith(indexSet.getIndexPrefix())) {
                continue;
            }
            if (indexMeta.getState().equals(IndexMetaData.State.CLOSE)) {
                closedIndices.add(indexMeta.getIndex());
            }
        }
        return closedIndices;
    }

    public Set<String> getReopenedIndices(final IndexSet indexSet) {
        final Set<String> reopenedIndices = Sets.newHashSet();

        ClusterStateRequest csr = new ClusterStateRequest()
                .nodes(false)
                .routingTable(false)
                .blocks(false)
                .metaData(true);

        ClusterState state = c.admin().cluster().state(csr).actionGet().getState();

        UnmodifiableIterator<IndexMetaData> it = state.getMetaData().getIndices().valuesIt();

        while (it.hasNext()) {
            IndexMetaData indexMeta = it.next();
            // Only search in our indices.
            if (!indexMeta.getIndex().startsWith(indexSet.getIndexPrefix())) {
                continue;
            }
            if (checkForReopened(indexMeta)) {
                reopenedIndices.add(indexMeta.getIndex());
            }
        }
        return reopenedIndices;
    }

    @Nullable
    public IndexStatistics getIndexStats(String index) {
        final IndexStats indexStats;
        try {
            indexStats = indexStats(index);
        } catch (ElasticsearchException e) {
            return null;
        }

        if (indexStats == null) {
            return null;
        }

        final ImmutableList.Builder<ShardRouting> shardRouting = ImmutableList.builder();
        for (ShardStats shardStats : indexStats.getShards()) {
            shardRouting.add(shardStats.getShardRouting());
        }

        return IndexStatistics.create(indexStats.getIndex(), indexStats.getPrimaries(), indexStats.getTotal(), shardRouting.build());
    }

    public Set<IndexStatistics> getIndicesStats(final IndexSet indexSet) {
        final Map<String, IndexStats> responseIndices;
        try {
            responseIndices = getAll(indexSet);
        } catch (ElasticsearchException e) {
            return Collections.emptySet();
        }

        final ImmutableSet.Builder<IndexStatistics> result = ImmutableSet.builder();
        for (IndexStats indexStats : responseIndices.values()) {
            final ImmutableList.Builder<ShardRouting> shardRouting = ImmutableList.builder();
            for (ShardStats shardStats : indexStats.getShards()) {
                shardRouting.add(shardStats.getShardRouting());
            }

            final IndexStatistics stats = IndexStatistics.create(
                    indexStats.getIndex(),
                    indexStats.getPrimaries(),
                    indexStats.getTotal(),
                    shardRouting.build());

            result.add(stats);
        }

        return result.build();
    }

    public boolean cycleAlias(String aliasName, String targetIndex) {
        return c.admin().indices().prepareAliases()
                .addAlias(targetIndex, aliasName)
                .execute().actionGet().isAcknowledged();
    }

    public boolean cycleAlias(String aliasName, String targetIndex, String oldIndex) {
        return c.admin().indices().prepareAliases()
                .removeAlias(oldIndex, aliasName)
                .addAlias(targetIndex, aliasName)
                .execute().actionGet().isAcknowledged();
    }

    public boolean removeAliases(String alias, Set<String> indices) {
        return c.admin().indices().prepareAliases()
                .removeAlias(indices.toArray(new String[0]), alias)
                .execute().actionGet().isAcknowledged();
    }

    public void optimizeIndex(String index, int maxNumSegments, Duration timeout) {
        // https://www.elastic.co/guide/en/elasticsearch/reference/2.1/indices-forcemerge.html
        final ForceMergeRequest request = c.admin().indices().prepareForceMerge(index)
                .setMaxNumSegments(maxNumSegments)
                .setOnlyExpungeDeletes(false)
                .setFlush(true)
                .request();

        // Using a specific timeout to override the global Elasticsearch request timeout
        c.admin().indices().forceMerge(request).actionGet(timeout.getQuantity(), timeout.getUnit());
    }

    public ClusterHealthStatus waitForRecovery(String index) {
        return waitForStatus(index, ClusterHealthStatus.YELLOW);
    }

    public ClusterHealthStatus waitForStatus(String index, ClusterHealthStatus clusterHealthStatus) {
        final ClusterHealthRequest request = c.admin().cluster().prepareHealth(index)
                .setWaitForStatus(clusterHealthStatus)
                .request();

        LOG.debug("Waiting until index health status of index {} is {}", index, clusterHealthStatus);

        final ClusterHealthResponse response = c.admin().cluster().health(request).actionGet(5L, TimeUnit.MINUTES);
        return response.getStatus();
    }

    @Nullable
    public DateTime indexCreationDate(String index) {
        final GetIndexRequest indexRequest = c.admin().indices().prepareGetIndex()
                .addFeatures(GetIndexRequest.Feature.SETTINGS)
                .addIndices(index)
                .request();
        try {
            final GetIndexResponse response = c.admin().indices()
                    .getIndex(indexRequest).actionGet();
            final Settings settings = response.settings().get(index);
            if (settings == null) {
                return null;
            }
            return new DateTime(settings.getAsLong("index.creation_date", 0L), DateTimeZone.UTC);
        } catch (ElasticsearchException e) {
            LOG.warn("Unable to read creation_date for index " + index, e.getRootCause());
            return null;
        }
    }

    /**
     * Calculate min and max message timestamps in the given index.
     *
     * @param index Name of the index to query.
     * @return the timestamp stats in the given index, or {@code null} if they couldn't be calculated.
     * @see org.elasticsearch.search.aggregations.metrics.stats.Stats
     */
    public IndexRangeStats indexRangeStatsOfIndex(String index) {
        final FilterAggregationBuilder builder = AggregationBuilders.filter("agg")
                .filter(QueryBuilders.existsQuery("timestamp"))
                .subAggregation(AggregationBuilders.min("ts_min").field("timestamp"))
                .subAggregation(AggregationBuilders.max("ts_max").field("timestamp"))
                .subAggregation(AggregationBuilders.terms("streams").field("streams"));
        final SearchRequestBuilder srb = c.prepareSearch()
                .setIndices(index)
                .setSearchType(SearchType.QUERY_THEN_FETCH)
                .setSize(0)
                .addAggregation(builder);

        final SearchResponse response;
        try {
            final SearchRequest request = srb.request();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Index range query: _search/{}: {}",
                        index,
                        XContentHelper.convertToJson(request.source(), false));
            }
            response = c.search(request).actionGet();
        } catch (IndexClosedException e) {
            throw e;
        } catch (org.elasticsearch.index.IndexNotFoundException e) {
            LOG.error("Error while calculating timestamp stats in index <" + index + ">", e);
            throw e;
        } catch (ElasticsearchException e) {
            LOG.error("Error while calculating timestamp stats in index <" + index + ">", e);
            throw new org.elasticsearch.index.IndexNotFoundException("Index " + index + " not found", e);
        } catch (IOException e) {
            // can potentially happen when recreating the source of
            // the index range aggregation query on DEBUG (via XContentHelper)
            throw new RuntimeException(e);
        }

        final Filter f = response.getAggregations().get("agg");
        if (f.getDocCount() == 0L) {
            LOG.debug("No documents with attribute \"timestamp\" found in index <{}>", index);
            return IndexRangeStats.EMPTY;
        }

        final Min minAgg = f.getAggregations().get("ts_min");
        final DateTime min = new DateTime((long) minAgg.getValue(), DateTimeZone.UTC);
        final Max maxAgg = f.getAggregations().get("ts_max");
        final DateTime max = new DateTime((long) maxAgg.getValue(), DateTimeZone.UTC);
        // make sure we return an empty list, so we can differentiate between old indices that don't have this information
        // and newer ones that simply have no streams.
        ImmutableList.Builder<String> streamIds = ImmutableList.builder();
        final Terms streams = f.getAggregations().get("streams");
        if (!streams.getBuckets().isEmpty()) {
            streamIds.addAll(streams.getBuckets().stream()
                    .map(Terms.Bucket::getKeyAsString)
                    .collect(toSet()));
        }

        return IndexRangeStats.create(min, max, streamIds.build());
    }
}
