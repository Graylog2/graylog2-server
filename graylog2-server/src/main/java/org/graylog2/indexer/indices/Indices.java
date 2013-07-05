/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
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
 *
 */

package org.graylog2.indexer.indices;

import com.beust.jcommander.internal.Maps;
import com.google.common.collect.ImmutableMap;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.admin.cluster.state.ClusterStateRequest;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.IndicesGetAliasesRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.admin.indices.stats.IndexStats;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsRequest;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.count.CountRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.support.replication.ReplicationType;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.graylog2.Core;
import org.graylog2.indexer.IndexNotFoundException;
import org.graylog2.indexer.Indexer;
import org.graylog2.indexer.Mapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Indices {

    private static final Logger LOG = LoggerFactory.getLogger(Indices.class);

    private final Core server;
    private final Client c;

    public Indices(Client client, Core server) {
        this.server = server;
        this.c = client;
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
                        new Object[] { source, target, response.getItems().length, response.getTookInMillis(), response.hasFailures() });

                if (response.hasFailures()) {
                    throw new RuntimeException("Failed to move a message. Check your indexer log.");
                }
            }
        }

    }

    public void delete(String indexName) {
        c.admin().indices().delete(new DeleteIndexRequest(indexName)).actionGet();
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
        return server.getConfiguration().getElasticSearchIndexPrefix() + "_*";
    }

    public boolean exists(String index) {
        ActionFuture<IndicesExistsResponse> existsFuture = c.admin().indices().exists(new IndicesExistsRequest(index));
        return existsFuture.actionGet().isExists();
    }

    public boolean aliasExists(String alias) {
        return c.admin().indices().existsAliases(new IndicesGetAliasesRequest(alias)).actionGet().exists();
    }

    public String aliasTarget(String alias) {
        // The ES return value of this has an awkward format: The first key of the hash is the target index. Thanks.
        return c.admin().indices().getAliases(new IndicesGetAliasesRequest(alias)).actionGet().getAliases().keySet().iterator().next();
    }

    public boolean create(String indexName) {
        Map<String, Integer> settings = Maps.newHashMap();
        settings.put("number_of_shards", server.getConfiguration().getElasticSearchShards());
        settings.put("number_of_replicas", server.getConfiguration().getElasticSearchReplicas());

        CreateIndexRequest cir = new CreateIndexRequest(indexName);
        cir.settings(settings);

        final ActionFuture<CreateIndexResponse> createFuture = c.admin().indices().create(cir);
        final boolean acknowledged = createFuture.actionGet().isAcknowledged();
        if (!acknowledged) {
            return false;
        }
        final PutMappingRequest mappingRequest = Mapping.getPutMappingRequest(c, indexName, server.getConfiguration().getElasticSearchAnalyzer());
        final boolean mappingCreated = c.admin().indices().putMapping(mappingRequest).actionGet().isAcknowledged();
        return acknowledged && mappingCreated;
    }

    public ImmutableMap<String, IndexMetaData> getMetadata() {
        return ImmutableMap.copyOf(c.admin().cluster().state(new ClusterStateRequest()).actionGet().getState().getMetaData().indices());
    }

    private IndexRequestBuilder manualIndexRequest(String index, Map<String, Object> doc, String id) {
        final IndexRequestBuilder b = new IndexRequestBuilder(c);
        b.setIndex(index);
        b.setId(id);
        b.setSource(doc);
        b.setOpType(IndexRequest.OpType.INDEX);
        b.setType(Indexer.TYPE);
        b.setConsistencyLevel(WriteConsistencyLevel.ONE);

        return b;
    }

}
