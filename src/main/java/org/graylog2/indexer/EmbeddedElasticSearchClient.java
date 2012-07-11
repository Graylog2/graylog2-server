package org.graylog2.indexer;

import com.beust.jcommander.internal.Maps;
import static org.elasticsearch.index.query.QueryBuilders.rangeQuery;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.exists.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.deletebyquery.DeleteByQueryRequestBuilder;
import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse;
import org.elasticsearch.action.index.IndexRequest.OpType;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;
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

    final static Calendar cal = Calendar.getInstance();

    private GraylogServer server;

    public EmbeddedElasticSearchClient(GraylogServer graylogServer) {
        server = graylogServer;

        final NodeBuilder builder = nodeBuilder().client(true);
        String esSettings;
        Map<String, String> settings;
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

    public boolean indexExists(String index) {
        ActionFuture<IndicesExistsResponse> existsFuture = client.admin().indices().exists(new IndicesExistsRequest(index));
        return existsFuture.actionGet().exists();
    }

    public boolean createIndex() {
        final ActionFuture<CreateIndexResponse> createFuture = client.admin().indices().create(new CreateIndexRequest(getMainIndexName()));
        final boolean acknowledged = createFuture.actionGet().acknowledged();
        if (!acknowledged) {
            return false;
        }
        final PutMappingRequest mappingRequest = Mapping.getPutMappingRequest(client, getMainIndexName());
        final boolean mappingCreated = client.admin().indices().putMapping(mappingRequest).actionGet().acknowledged();
        return acknowledged && mappingCreated;
    }
    
    public boolean createRecentIndex() {
        Map<String, Object> settings = Maps.newHashMap();
        settings.put("index.store.type", "memory");
        
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

    public boolean bulkIndex(final List<LogMessage> messages) {
        if (messages.isEmpty()) {
            return true;
        }

        final BulkRequestBuilder b = client.prepareBulk();
        for (LogMessage msg : messages) {
            String source = JSONValue.toJSONString(msg.toElasticSearchObject());
            
            b.add(buildIndexRequest(getMainIndexName(), source, 0)); // Main index.
            b.add(buildIndexRequest(RECENT_INDEX_NAME, source, server.getConfiguration().getRecentIndexTtlMinutes())); // Recent index.
        }

        final ActionFuture<BulkResponse> bulkFuture = client.bulk(b.request());
        final BulkResponse response = bulkFuture.actionGet();
        LOG.debug(String.format("Bulk indexed %d messages, took %d ms, failures: %b",
                response.items().length,
                response.getTookInMillis(),
                response.hasFailures()));
        return !response.hasFailures();
    }

    public boolean deleteMessagesByTimeRange(int to) {
        DeleteByQueryRequestBuilder b = client.prepareDeleteByQuery( getMainIndexName() );
        b.setTypes( TYPE );
        final QueryBuilder qb = rangeQuery("created_at").from(0).to(to);
        b.setQuery(qb);
        ActionFuture<DeleteByQueryResponse> future = client.deleteByQuery(b.request());
        return future.actionGet().index(getMainIndexName()).failedShards() == 0;
    }

    public String getMainIndexName() {
        return server.getConfiguration().getElasticSearchIndexName();
    }

    // yyyy-MM-dd HH-mm-ss
    // http://docs.oracle.com/javase/1.5.0/docs/api/java/util/Formatter.html#syntax
    public static String buildTimeFormat(double timestamp) {
        cal.setTimeInMillis(System.currentTimeMillis());

        return String.format("%1$tY-%1$tm-%1$td %1$tH-%1$tM-%1$tS", cal); // ramtamtam
    }
    
    private IndexRequestBuilder buildIndexRequest(String index, String source, int ttlMinutes) {
        final IndexRequestBuilder b = new IndexRequestBuilder(client);
        
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
