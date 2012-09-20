package org.graylog2.indexer;

import com.beust.jcommander.internal.Maps;
import static org.elasticsearch.index.query.QueryBuilders.rangeQuery;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoRequest;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.exists.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.admin.indices.stats.IndexStats;
import org.elasticsearch.action.admin.indices.stats.IndicesStats;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.deletebyquery.DeleteByQueryRequestBuilder;
import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexRequest.OpType;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.UUID;
import org.elasticsearch.common.settings.loader.YamlSettingsLoader;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.graylog2.GraylogServer;
import org.graylog2.logmessage.LogMessage;
import org.json.simple.JSONValue;

// TODO this class blocks for most of its operations, but is called from the main thread for some of them
// TODO figure out how to gracefully deal with failure to connect (or losing connection) to the elastic search cluster!
public class EmbeddedElasticSearchClient {
    private static final Logger LOG = Logger.getLogger(EmbeddedElasticSearchClient.class);

    private Client client;
    public static final String TYPE = "message";
    public static final String RECENT_INDEX_NAME = "graylog2_recent";
    
    // http://www.elasticsearch.org/guide/reference/index-modules/store.html
    public static final String STANDARD_RECENT_INDEX_STORE_TYPE = "niofs";
    public static final List<String> ALLOWED_RECENT_INDEX_STORE_TYPES = new ArrayList<String>() {{ 
        add("niofs");
        add("simplefs");
        add("mmapfs");
        add("memory");
    }};

    final static Calendar cal = Calendar.getInstance();

    private GraylogServer server;

    public EmbeddedElasticSearchClient(GraylogServer graylogServer) {
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

    }
    
    public String allIndicesAlias() {

        return server.getConfiguration().getElasticSearchIndexPrefix() + "_*";
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
    
    public Map<String, IndexStats> getIndices() {
        ActionFuture<IndicesStats> isr = client.admin().indices().stats(new IndicesStatsRequest().all());
        
        return isr.actionGet().indices();
    }

    public boolean indexExists(String index) {
        ActionFuture<IndicesExistsResponse> existsFuture = client.admin().indices().exists(new IndicesExistsRequest(index));
        return existsFuture.actionGet().exists();
    }

    public boolean createIndex(String indexName) {
        final ActionFuture<CreateIndexResponse> createFuture = client.admin().indices().create(new CreateIndexRequest(indexName));
        final boolean acknowledged = createFuture.actionGet().acknowledged();
        if (!acknowledged) {
            return false;
        }
        final PutMappingRequest mappingRequest = Mapping.getPutMappingRequest(client, indexName);
        final boolean mappingCreated = client.admin().indices().putMapping(mappingRequest).actionGet().acknowledged();
        return acknowledged && mappingCreated;
    }
    
    public boolean createRecentIndex() {
        Map<String, Object> settings = Maps.newHashMap();
        settings.put("index.store.type", server.getConfiguration().getRecentIndexStoreType());
        
        CreateIndexRequestBuilder crb = new CreateIndexRequestBuilder(client.admin().indices());
        crb.setIndex(RECENT_INDEX_NAME);
        crb.setSettings(settings);
        
        final ActionFuture<CreateIndexResponse> createFuture = client.admin().indices().create(crb.request());
        final boolean acknowledged = createFuture.actionGet().acknowledged();
        if (!acknowledged) {
            return false;
        }
        final PutMappingRequest mappingRequest = Mapping.getPutMappingRequest(client, RECENT_INDEX_NAME);
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
        
        return index.getTotal().docs().count();
    }
    
    public boolean index(final LogMessage msg) {
        String source = JSONValue.toJSONString(msg.toElasticSearchObject());
        final String id = UUID.randomBase64UUID();
        client.index(buildIndexRequest(Deflector.DEFLECTOR_NAME, source, id, 0).request()).actionGet();
        client.index(buildIndexRequest(RECENT_INDEX_NAME, source, id, server.getConfiguration().getRecentIndexTtlMinutes()).request()).actionGet();
        return true;
    }

    public boolean bulkIndex(final List<LogMessage> messages) {
        if (messages.isEmpty()) {
            return true;
        }

        final BulkRequestBuilder b = client.prepareBulk();
        for (LogMessage msg : messages) {
            String source = JSONValue.toJSONString(msg.toElasticSearchObject());
            
            // We manually set the same ID to allow linking between indices later.
            final String id = UUID.randomBase64UUID();
            
            b.add(buildIndexRequest(Deflector.DEFLECTOR_NAME, source, id, 0)); // Main index.
            b.add(buildIndexRequest(RECENT_INDEX_NAME, source, id, server.getConfiguration().getRecentIndexTtlMinutes())); // Recent index.
        }

        final ActionFuture<BulkResponse> bulkFuture = client.bulk(b.request());
        final BulkResponse response = bulkFuture.actionGet();
        LOG.debug(String.format("Bulk indexed %d messages, took %d ms, failures: %b",
                response.items().length,
                response.getTookInMillis(),
                response.hasFailures()));
        return !response.hasFailures();
    }

    public void deleteMessagesByTimeRange(int to) {
        DeleteByQueryRequestBuilder b = client.prepareDeleteByQuery();
        final QueryBuilder qb = rangeQuery("created_at").from(0).to(to);
        
        b.setTypes(new String[] {TYPE});
        b.setIndices(server.getDeflector().getAllIndexNames());
        b.setQuery(qb);
        
        ActionFuture<DeleteByQueryResponse> future = client.deleteByQuery(b.request());
        future.actionGet();
    }

    // yyyy-MM-dd HH-mm-ss
    // http://docs.oracle.com/javase/1.5.0/docs/api/java/util/Formatter.html#syntax
    public static String buildTimeFormat(double timestamp) {
        cal.setTimeInMillis(System.currentTimeMillis());

        return String.format("%1$tY-%1$tm-%1$td %1$tH-%1$tM-%1$tS", cal); // ramtamtam
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
        
        // Set a TTL?
        if (ttlMinutes > 0) {
            b.setTTL(ttlMinutes*60*1000); // TTL is specified in milliseconds.
        }
        
        return b;
    }
}
