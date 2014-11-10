/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.indexer.indices;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.admin.cluster.state.ClusterStateRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.close.CloseIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.flush.FlushRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.admin.indices.open.OpenIndexRequest;
import org.elasticsearch.action.admin.indices.optimize.OptimizeRequest;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.admin.indices.stats.IndexStats;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsRequest;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsResponse;
import org.elasticsearch.action.admin.indices.stats.ShardStats;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.count.CountRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.support.replication.ReplicationType;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.UnmodifiableIterator;
import org.elasticsearch.common.hppc.cursors.ObjectObjectCursor;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.node.Node;
import org.elasticsearch.search.SearchHit;
import org.graylog2.Configuration;
import org.graylog2.indexer.IndexNotFoundException;
import org.graylog2.indexer.Mapping;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.plugin.indexer.retention.IndexManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

@Singleton
public class Indices implements IndexManagement {

    private static final Logger LOG = LoggerFactory.getLogger(Indices.class);

    private final Client c;
    private final Configuration configuration;

    @Inject
    public Indices(Node node, Configuration configuration) {
        this.c = node.client();
        this.configuration = configuration;
    }

    public void move(String source, String target) {
        QueryBuilder qb = matchAllQuery();

        SearchResponse scrollResp = c.prepareSearch(source)
                .setSearchType(SearchType.SCAN)
                .setScroll(new TimeValue(10000))
                .setQuery(qb)
                .setSize(350).execute().actionGet();

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

                request.add(manualIndexRequest(target, doc, id).request());
            }

            request.setConsistencyLevel(WriteConsistencyLevel.ONE);
            request.setReplicationType(ReplicationType.ASYNC);

            if (request.numberOfActions() > 0) {
                BulkResponse response = c.bulk(request.request()).actionGet();

                LOG.info("Moving index <{}> to <{}>: Bulk indexed {} messages, took {} ms, failures: {}",
                        source, target, response.getItems().length, response.getTookInMillis(), response.hasFailures());

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
        Map<String, IndexStats> indices = getAll();
        IndexStats index = indices.get(indexName);

        if (index == null) {
            throw new IndexNotFoundException();
        }

        return index.getPrimaries().getDocs().getCount();
    }

    public Map<String, IndexStats> getAll() {
        ActionFuture<IndicesStatsResponse> isr = c.admin().indices().stats(new IndicesStatsRequest().all());

        return isr.actionGet().getIndices();
    }

    public long getTotalNumberOfMessages() {
        return c.count(new CountRequest(allIndicesAlias())).actionGet().getCount();
    }

    public long getTotalSize() {
        return c.admin().indices().stats(
                new IndicesStatsRequest().indices(allIndicesAlias()))
                .actionGet()
                .getTotal()
                .getStore()
                .getSize()
                .getMb();
    }

    public String allIndicesAlias() {
        return configuration.getElasticSearchIndexPrefix() + "_*";
    }

    public boolean exists(String index) {
        ActionFuture<IndicesExistsResponse> existsFuture = c.admin().indices().exists(new IndicesExistsRequest(index));
        return existsFuture.actionGet().isExists();
    }

    public boolean aliasExists(String alias) {
        return c.admin().indices().aliasesExist(new GetAliasesRequest(alias)).actionGet().exists();
    }

    public String aliasTarget(String alias) {
        // The ES return value of this has an awkward format: The first key of the hash is the target index. Thanks.
        return c.admin().indices().getAliases(new GetAliasesRequest(alias)).actionGet().getAliases().keysIt().next();
    }

    public boolean create(String indexName) {
        Map<String, Object> settings = Maps.newHashMap();
        settings.put("number_of_shards", configuration.getElasticSearchShards());
        settings.put("number_of_replicas", configuration.getElasticSearchReplicas());
        Map<String, String> keywordLowercase = Maps.newHashMap();
        keywordLowercase.put("tokenizer", "keyword");
        keywordLowercase.put("filter", "lowercase");
        settings.put("index.analysis.analyzer.analyzer_keyword", keywordLowercase);

        CreateIndexRequest cir = new CreateIndexRequest(indexName);
        cir.settings(settings);

        final ActionFuture<CreateIndexResponse> createFuture = c.admin().indices().create(cir);
        final boolean acknowledged = createFuture.actionGet().isAcknowledged();
        if (!acknowledged) {
            return false;
        }
        final PutMappingRequest mappingRequest = Mapping.getPutMappingRequest(c, indexName, configuration.getElasticSearchAnalyzer());
        return c.admin().indices().putMapping(mappingRequest).actionGet().isAcknowledged();
    }

    public ImmutableMap<String, IndexMetaData> getMetadata() {
        Map<String, IndexMetaData> metaData = Maps.newHashMap();

        for (ObjectObjectCursor<String, IndexMetaData> next : c.admin().cluster().state(new ClusterStateRequest()).actionGet().getState().getMetaData().indices()) {
            metaData.put(next.key, next.value);
        }

        return ImmutableMap.copyOf(metaData);
    }

    public Set<String> getAllMessageFields() {
        Set<String> fields = Sets.newHashSet();

        ClusterStateRequest csr = new ClusterStateRequest().blocks(true).nodes(true).indices(allIndicesAlias());
        ClusterState cs = c.admin().cluster().state(csr).actionGet().getState();

        for (ObjectObjectCursor<String, IndexMetaData> m : cs.getMetaData().indices()) {
            try {
                MappingMetaData mmd = m.value.mapping(Messages.TYPE);
                if (mmd == null) {
                    // There is no mapping if there are no messages in the index.
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> mapping = (Map<String, Object>) mmd.getSourceAsMap().get("properties");

                fields.addAll(mapping.keySet());
            } catch (Exception e) {
                LOG.error("Error while trying to get fields of <" + m.index + ">", e);
            }
        }

        return fields;
    }

    private IndexRequestBuilder manualIndexRequest(String index, Map<String, Object> doc, String id) {
        final IndexRequestBuilder b = new IndexRequestBuilder(c);
        b.setIndex(index);
        b.setId(id);
        b.setSource(doc);
        b.setOpType(IndexRequest.OpType.INDEX);
        b.setType(Messages.TYPE);
        b.setConsistencyLevel(WriteConsistencyLevel.ONE);

        return b;
    }

    public void setReadOnly(String index) {
        ImmutableSettings.Builder sb = ImmutableSettings.builder();

        // http://www.elasticsearch.org/guide/reference/api/admin-indices-update-settings/
        sb.put("index.blocks.write", true); // Block writing.
        sb.put("index.blocks.read", false); // Allow reading.
        sb.put("index.blocks.metadata", false); // Allow getting metadata.

        c.admin().indices().updateSettings(new UpdateSettingsRequest(index).settings(sb.build())).actionGet();
    }

    public void flush(String index) {
        FlushRequest flush = new FlushRequest(index);
        flush.force(true); // Just flushes. Even if it is not necessary.
        flush.full(false);

        c.admin().indices().flush(new FlushRequest(index).force(true)).actionGet();
    }

    public void reopenIndex(String index) {
        // Mark this index as re-opened. It will never be touched by retention.
        UpdateSettingsRequest settings = new UpdateSettingsRequest(index);
        settings.settings(Collections.<String, Object>singletonMap("graylog2_reopened", true));
        c.admin().indices().updateSettings(settings).actionGet();

        // Open index.
        c.admin().indices().open(new OpenIndexRequest(index)).actionGet();
    }

    public boolean isReopened(String indexName) {
        ClusterState clusterState = c.admin().cluster().state(new ClusterStateRequest()).actionGet().getState();
        IndexMetaData metaData = clusterState.getMetaData().getIndices().get(indexName);

        if (metaData == null) {
            return false;
        }

        return metaData.getSettings().getAsBoolean("index.graylog2_reopened", false);
    }

    public Set<String> getClosedIndices() {
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
            if (!indexMeta.getIndex().startsWith(configuration.getElasticSearchIndexPrefix())) {
                continue;
            }
            if (indexMeta.getState().equals(IndexMetaData.State.CLOSE)) {
                closedIndices.add(indexMeta.getIndex());
            }
        }
        return closedIndices;
    }

    public IndexStatistics getIndexStats(String index) {
        final IndexStatistics stats = new IndexStatistics();
        try {
            IndicesStatsResponse indicesStatsResponse = c.admin().indices().stats(new IndicesStatsRequest().all()).actionGet();
            IndexStats indexStats = indicesStatsResponse.getIndex(index);

            if (indexStats == null) {
                return null;
            }
            stats.setPrimaries(indexStats.getPrimaries());
            stats.setTotal(indexStats.getTotal());

            for (ShardStats shardStats : indexStats.getShards()) {
                stats.addShardRouting(shardStats.getShardRouting());
            }
        } catch (ElasticsearchException e) {
            return null;
        }
        return stats;
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

    public void optimizeIndex(String index) {
        // http://www.elasticsearch.org/guide/reference/api/admin-indices-optimize/
        OptimizeRequest or = new OptimizeRequest(index);

        or.maxNumSegments(configuration.getIndexOptimizationMaxNumSegments());
        or.onlyExpungeDeletes(false);
        or.flush(true);
        or.waitForMerge(true); // This makes us block until the operation finished.

        c.admin().indices().optimize(or).actionGet();
    }
}
