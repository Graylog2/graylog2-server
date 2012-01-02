package org.graylog2.indexer;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.exists.IndicesExistsResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.deletebyquery.DeleteByQueryRequest;
import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse;
import org.elasticsearch.action.deletebyquery.IndexDeleteByQueryResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.client.action.admin.indices.exists.IndicesExistsRequestBuilder;
import org.elasticsearch.client.action.bulk.BulkRequestBuilder;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.graylog2.Tools;
import org.graylog2.messagehandlers.gelf.GELFMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class ElasticSearchIndexer implements Indexer {

    private static final Logger LOG = Logger.getLogger(ElasticSearchIndexer.class);

    private Client elasticSearchClient;
    private String indexName;
    private String indexType;

    public ElasticSearchIndexer(Client elasticSearchClient, String indexName, String indexType) throws Exception {
        this.elasticSearchClient = elasticSearchClient;
        this.indexName = indexName;
        this.indexType = indexType;
        
        if(!indexExists()) {
                createIndex();
        }
    }

    /**
     * Checks if the index for Graylog2 exists
     * <p/>
     * See <a href="http://www.elasticsearch.org/guide/reference/api/admin-indices-indices-exists.html">elasticsearch Indices Exists API</a> for details.
     *
     * @return {@literal true} if the index for Graylog2 exists, {@literal false} otherwise
     */
    private boolean indexExists() {

        IndicesExistsRequestBuilder builder = elasticSearchClient.admin().indices().prepareExists(indexName);

        ListenableActionFuture<IndicesExistsResponse> response = builder.execute();

        return response.actionGet().exists();
    }

    /**
     * Creates the index for Graylog2 including the mapping
     * <p/>
     * <a href="http://www.elasticsearch.org/guide/reference/api/admin-indices-create-index.html">Create Index API</a> and
     * <a href="http://www.elasticsearch.org/guide/reference/mapping">elasticsearch Mapping</a>
     *
     * @return {@literal true} if the index for Graylog2 could be created, {@literal false} otherwise
     * @throws IOException if elasticsearch server couldn't be reached
     */
    private boolean createIndex() throws IOException {

        CreateIndexRequestBuilder builder = elasticSearchClient.admin().indices().prepareCreate(indexName);

        ListenableActionFuture<CreateIndexResponse> response = builder.addMapping(indexType, Mapping.get()).execute();

        return response.actionGet().acknowledged();
    }

    /**
     * Bulk-indexes/persists messages to ElasticSearch.
     * <p/>
     * See <a href="http://www.elasticsearch.org/guide/reference/api/bulk.html">elasticsearch Bulk API</a> for details
     *
     * @param messages The messages to index
     * @return {@literal true} if the messages were successfully indexed, {@literal false} otherwise
     */
    @Override
    public boolean bulkIndex(List<GELFMessage> messages) {

        if (messages.isEmpty()) {
            return true;
        }
        
        BulkRequestBuilder bulkRequest = elasticSearchClient.prepareBulk();
        
        for(GELFMessage gelfMessage : messages) {

            double createdAt = gelfMessage.getCreatedAt();
            
            if (createdAt <= 0.0d) {
                createdAt = Tools.getUTCTimestampWithMilliseconds();
            }
            
            // Manually converting stream ID to string - caused strange problems without it.
            List<String> streamIds = new ArrayList<String>();
            for (ObjectId id : gelfMessage.getStreamIds()) {
                streamIds.add(id.toString());
            }
            
            try {
                XContentBuilder contentBuilder = jsonBuilder()
                        .startObject()
                        .field("message", gelfMessage.getShortMessage())
                        .field("full_message", gelfMessage.getFullMessage())
                        .field("file", gelfMessage.getFile())
                        .field("line", gelfMessage.getLine())
                        .field("host", gelfMessage.getHost())
                        .field("facility", gelfMessage.getFacility())
                        .field("level", gelfMessage.getLevel())
                        .field("created_at", createdAt)
                        .field("streams", streamIds);
                
                for(Map.Entry<String, Object> entry : gelfMessage.getAdditionalData().entrySet()) {
                    
                    contentBuilder.field(entry.getKey(), entry.getValue());
                }
                        
                contentBuilder.endObject();

                bulkRequest.add(elasticSearchClient.prepareIndex(indexName, indexType).setSource(contentBuilder));
            } catch (IOException e) {
                LOG.warn("IO error when trying to index messages", e);
            }
        }
        
        ListenableActionFuture<BulkResponse> futureResponse = bulkRequest.execute(); 
        BulkResponse bulkResponse = futureResponse.actionGet();
        
        if(futureResponse.isCancelled()) {
            return false;
        } else {
            if (bulkResponse.hasFailures()) {
                
                LOG.warn(String.format("%d errors while bulk indexing messages", bulkResponse.items().length));
                
                if(LOG.isDebugEnabled()) {
                    for(BulkItemResponse responseItem : bulkResponse.items()) {
                        
                        LOG.debug("Failure indexing message: " + responseItem.failureMessage());
                    }
                }
            }
            
            return true;
        }
    }

    /**
     * Deletes all messages from index which are older than the specified timestamp.
     *
     * @param to UTC UNIX timestamp
     * @return {@literal true} if the messages were successfully deleted, {@literal false} otherwise
     */
    @Override
    public boolean deleteMessagesByTimeRange(int to) {
        DeleteByQueryRequest request = Requests.deleteByQueryRequest(indexName).query(
                QueryBuilders.rangeQuery("created_at").from(0).to(to));
        
        ActionFuture<DeleteByQueryResponse> futureResponse = elasticSearchClient.deleteByQuery(request);

        DeleteByQueryResponse response = futureResponse.actionGet();
        
        if(futureResponse.isCancelled()) {
            return false;
        } else {

            if (LOG.isDebugEnabled()) {
                IndexDeleteByQueryResponse indexDeleteByQueryResponse = response.index(indexName);

                LOG.debug(
                        String.format("Finished message removal; successful/failed/total shards: %d/%d/%d",
                                indexDeleteByQueryResponse.successfulShards(),
                                indexDeleteByQueryResponse.failedShards(),
                                indexDeleteByQueryResponse.totalShards()
                        )
                );
            }

            return true;
        }
    }
}
