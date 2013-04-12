package org.graylog2.indexer;

import com.beust.jcommander.internal.Maps;
import org.apache.commons.io.FileUtils;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoRequest;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.action.admin.cluster.state.ClusterStateRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.admin.indices.stats.IndexStats;
import org.elasticsearch.action.admin.indices.stats.IndicesStats;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.count.CountRequest;
import org.elasticsearch.action.deletebyquery.DeleteByQueryRequestBuilder;
import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse;
import org.elasticsearch.action.index.IndexRequest.OpType;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.support.replication.ReplicationType;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.settings.loader.YamlSettingsLoader;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.graylog2.Core;
import org.graylog2.activities.Activity;
import org.graylog2.indexer.counts.Counts;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.plugin.Message;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.index.query.QueryBuilders.rangeQuery;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;
import org.graylog2.plugin.indexer.MessageGateway;

// TODO this class blocks for most of its operations, but is called from the main thread for some of them
// TODO figure out how to gracefully deal with failure to connect (or losing connection) to the elastic search cluster!
public class Indexer {
    private static final Logger LOG = LoggerFactory.getLogger(Indexer.class);

    private Client client;
    private final MessageGateway messageGateway;
    public static final String TYPE = "message";
    
    private final Searches searches;
    private final Counts counts;
    private final Messages messages;
    
	public static enum DateHistogramInterval {
		YEAR, QUARTER, MONTH, WEEK, DAY, HOUR, MINUTE
	}

    @SuppressWarnings("serial")
	public static final List<String> ALLOWED_RECENT_INDEX_STORE_TYPES = new ArrayList<String>() {{ 
        add("niofs");
        add("simplefs");
        add("mmapfs");
        add("memory");
    }};

    private Core server;

    public Indexer(Core graylogServer) {
        server = graylogServer;

        final NodeBuilder builder = nodeBuilder().client(true);
        String esSettings;
        Map<String, String> settings = null;
        try {
            esSettings = FileUtils.readFileToString(new File(graylogServer.getConfiguration().getElasticSearchConfigFile()));
            settings = new YamlSettingsLoader().load(esSettings);
        } catch (IOException e) {
            throw new RuntimeException("Cannot read elasticsearch configuration.", e);
        }
        builder.settings().put(settings);
        final Node node = builder.node();
        client = node.client();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                node.close();
            }
        });
        
        messageGateway = new MessageGatewayImpl(graylogServer);
        searches = new Searches(client, graylogServer);
        counts = new Counts(client, graylogServer);
        messages = new Messages(client, graylogServer);
    }
    
    public Client getClient() {
        return client;
    }

    public MessageGateway getMessageGateway() {
        return messageGateway;
    }
    
    public String allIndicesAlias() {
        return server.getConfiguration().getElasticSearchIndexPrefix() + "_*";
    }
    
    public long getTotalIndexSize() {
        return client.admin().indices().stats(
                new IndicesStatsRequest().indices(allIndicesAlias()))
                .actionGet()
                .total()
                .store()
                .getSize()
                .getMb();
    }
    
    public String nodeIdToName(String nodeId) {
        if (nodeId == null || nodeId.isEmpty()) {
            return null;
        }
        
        try {
            NodesInfoResponse r = client.admin().cluster().nodesInfo(new NodesInfoRequest(nodeId).all()).actionGet();
            return r.getNodesMap().get(nodeId).getNode().getName();
        } catch (Exception e) {
            LOG.error("Could not read name of ES node.", e);
            return "UNKNOWN";
        }
        
    }
    
    public String nodeIdToHostName(String nodeId) {
        if (nodeId == null || nodeId.isEmpty()) {
            return null;
        }
        
        try {
            NodesInfoResponse r = client.admin().cluster().nodesInfo(new NodesInfoRequest(nodeId).all()).actionGet();
            return r.getNodesMap().get(nodeId).getHostname();
        } catch (Exception e) {
            LOG.error("Could not read name of ES node.", e);
            return "UNKNOWN";
        }
        
    }
    
    public int getNumberOfNodesInCluster() {
        return client.admin().cluster().nodesInfo(new NodesInfoRequest().all()).actionGet().nodes().length;
    }
    
    public long getTotalNumberOfMessagesInIndices() {
        return client.count(new CountRequest(allIndicesAlias())).actionGet().count();
    }
    
    public Map<String, IndexStats> getIndices() {
        ActionFuture<IndicesStats> isr = client.admin().indices().stats(new IndicesStatsRequest().all());
        
        return isr.actionGet().indices();
    }
    
    public ImmutableMap<String, IndexMetaData> getIndicesMetadata() {
        return client.admin().cluster().state(new ClusterStateRequest()).actionGet().getState().getMetaData().indices();
    }
    
    public boolean indexExists(String index) {
        ActionFuture<IndicesExistsResponse> existsFuture = client.admin().indices().exists(new IndicesExistsRequest(index));
        return existsFuture.actionGet().exists();
    }

    public boolean createIndex(String indexName) {
        Map<String, Integer> settings = Maps.newHashMap();
        settings.put("number_of_shards", server.getConfiguration().getElasticSearchShards());
        settings.put("number_of_replicas", server.getConfiguration().getElasticSearchReplicas());

        CreateIndexRequest cir = new CreateIndexRequest(indexName);
        cir.settings(settings);
        
        final ActionFuture<CreateIndexResponse> createFuture = client.admin().indices().create(cir);
        final boolean acknowledged = createFuture.actionGet().acknowledged();
        if (!acknowledged) {
            return false;
        }
        final PutMappingRequest mappingRequest = Mapping.getPutMappingRequest(client, indexName, server.getConfiguration().getElasticSearchAnalyzer());
        final boolean mappingCreated = client.admin().indices().putMapping(mappingRequest).actionGet().acknowledged();
        return acknowledged && mappingCreated;
    }
    
    public boolean cycleAlias(String aliasName, String targetIndex) {
        return client.admin().indices().prepareAliases()
                .addAlias(targetIndex, aliasName)
                .execute().actionGet().acknowledged();
    }
    
    public boolean cycleAlias(String aliasName, String targetIndex, String oldIndex) {
        return client.admin().indices().prepareAliases()
                .removeAlias(oldIndex, aliasName)
                .addAlias(targetIndex, aliasName)
                .execute().actionGet().acknowledged();
    }
    
    public long numberOfMessages(String indexName) throws IndexNotFoundException {
        Map<String, IndexStats> indices = getIndices();
        IndexStats index = indices.get(indexName);
        
        if (index == null) {
            throw new IndexNotFoundException();
        }
        
        return index.getPrimaries().docs().count();
    }

    public boolean bulkIndex(final List<Message> messages) {
        if (messages.isEmpty()) {
            return true;
        }

        final BulkRequestBuilder request = client.prepareBulk();
        for (Message msg : messages) {
            String source = JSONValue.toJSONString(msg.toElasticSearchObject());

            // we manually set the document ID to the same value to be able to match up documents later.
            request.add(buildIndexRequest(Deflector.DEFLECTOR_NAME, source, msg.getId(), 0)); // Main index.
        }

        request.setConsistencyLevel(WriteConsistencyLevel.ONE);
        request.setReplicationType(ReplicationType.ASYNC);
        
        final BulkResponse response = client.bulk(request.request()).actionGet();
        
        LOG.debug("Deflector index: Bulk indexed {} messages, took {} ms, failures: {}",
                new Object[] { response.items().length, response.getTookInMillis(), response.hasFailures() });

        return !response.hasFailures();
    }

    public void deleteMessagesByTimeRange(int to) {
        DeleteByQueryRequestBuilder b = client.prepareDeleteByQuery();
        final QueryBuilder qb = rangeQuery("created_at").from(0).to(to);
        
        b.setTypes(new String[] {TYPE});
        b.setIndices(server.getDeflector().getAllDeflectorIndexNames());
        b.setQuery(qb);
        
        ActionFuture<DeleteByQueryResponse> future = client.deleteByQuery(b.request());
        future.actionGet();
    }
    
    public void deleteIndex(String indexName) {
        client.admin().indices().delete(new DeleteIndexRequest(indexName)).actionGet();
    }
    
    public void runIndexRetention() throws NoTargetIndexException {
        Map<String, IndexStats> indices = server.getDeflector().getAllDeflectorIndices();
        int indexCount = indices.size();
        int maxIndices = server.getConfiguration().getMaxNumberOfIndices();
        
        // Do we have more indices than the configured maximum?
        if (indexCount <= maxIndices) {
            LOG.debug("Number of indices ({}) lower than limit ({}). Not performing any retention actions.",
                    indexCount, maxIndices);
            return;
        }
        
        // We have more indices than the configured maximum! Remove as many as needed.
        int remove = indexCount-maxIndices;
        String msg = "Number of indices (" + indexCount + ") higher than limit (" + maxIndices + "). Deleting " + remove + " indices.";
        LOG.info(msg);
        server.getActivityWriter().write(new Activity(msg, Indexer.class));
        
        for (String indexName : IndexHelper.getOldestIndices(indices.keySet(), remove)) {
            // Never delete the current deflector target.
            if (server.getDeflector().getCurrentTargetName().equals(indexName)) {
                LOG.info("Not deleting current deflector target <{}>.", indexName);
                continue;
            }
            
            msg = "Retention cleaning: Deleting index <" + indexName + ">";
            LOG.info(msg);
            server.getActivityWriter().write(new Activity(msg, Indexer.class));
            
            // Sorry if this should ever go mad. Delete the index!
            deleteIndex(indexName);
            server.getMongoBridge().removeIndexDateRange(indexName);
        }
    }
    
    public Searches searches() {
    	return searches;
    }
    
    public Counts counts() {
    	return counts;
    }
    
    public Messages messages() {
    	return messages;
    }
    
    private IndexRequestBuilder buildIndexRequest(String index, String source, String id, int ttlMinutes) {
        final IndexRequestBuilder b = new IndexRequestBuilder(client);
        
        /*
         * ID is set manually to allow inserting message into recent and total index
         * with same ID. (Required for linking in frontend)
         */
        b.setId(id);
        b.setSource(source);
        b.setIndex(index);
        b.setContentType(XContentType.JSON);
        b.setOpType(OpType.INDEX);
        b.setType(TYPE);
        b.setConsistencyLevel(WriteConsistencyLevel.ONE);

        // Set a TTL?
        if (ttlMinutes > 0) {
            b.setTTL(ttlMinutes*60*1000); // TTL is specified in milliseconds.
        }
        
        return b;
    }

}
