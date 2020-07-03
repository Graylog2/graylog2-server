package org.graylog.storage.elasticsearch7.testing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.cluster.settings.ClusterUpdateSettingsRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest.AliasActions;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.indices.template.delete.DeleteIndexTemplateRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.bulk.BulkRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.index.IndexRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.support.IndicesOptions;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.support.WriteRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.Request;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.Response;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.CloseIndexRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.CreateIndexRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.GetIndexRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.GetIndexResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.GetIndexTemplatesRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.GetIndexTemplatesResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.GetMappingsRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.GetMappingsResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.IndexTemplateMetaData;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.PutIndexTemplateRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.common.settings.Settings;
import org.graylog.storage.elasticsearch7.ElasticsearchClient;
import org.graylog.testing.elasticsearch.BulkIndexRequest;
import org.graylog.testing.elasticsearch.Client;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class ClientES7 implements Client {
    private final ElasticsearchClient client;
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    public ClientES7(ElasticsearchClient client) {
        this.client = client;
    }

    @Override
    public void createIndex(String index, int shards, int replicas) {
        final CreateIndexRequest createIndexRequest = new CreateIndexRequest(index);
        createIndexRequest.settings(
                Settings.builder()
                        .put("index.number_of_shards", shards)
                        .put("index.number_of_replicas", replicas)
        );

        client.execute((c, requestOptions) -> c.indices().create(createIndexRequest, requestOptions));
    }

    @Override
    public void deleteIndices(String... indices) {
        for (String index : indices)
            if (indicesExists(index)) {
                final DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(index);
                client.execute((c, requestOptions) -> c.indices().delete(deleteIndexRequest, requestOptions));
            }
    }

    @Override
    public void closeIndex(String index) {
        final CloseIndexRequest closeIndexRequest = new CloseIndexRequest(index);
        client.execute((c, requestOptions) -> c.indices().close(closeIndexRequest, requestOptions));
    }

    @Override
    public boolean indicesExists(String... indices) {
        final GetIndexRequest getIndexRequest = new GetIndexRequest(indices);
        return client.execute((c, requestOptions) -> c.indices().exists(getIndexRequest, requestOptions));
    }

    @Override
    public void addAliasMapping(String indexName, String alias) {
        final IndicesAliasesRequest indicesAliasesRequest = new IndicesAliasesRequest();
        final AliasActions aliasAction = new AliasActions(AliasActions.Type.ADD)
                .index(indexName)
                .alias(alias);
        indicesAliasesRequest.addAliasAction(aliasAction);

        client.execute((c, requestOptions) -> c.indices().updateAliases(indicesAliasesRequest, requestOptions),
                "failed to add alias " + alias + " for index " + indexName);
    }

    @Override
    public boolean isSourceEnabled(String testIndexName) {
        final GetMappingsResponse mapping = getMapping(testIndexName);
        return true;
    }

    @Override
    public String fieldType(String testIndexName, String source) {
        final GetMappingsResponse mapping = getMapping(testIndexName);
        return mapping.mappings().get("source").type();
    }

    private GetMappingsResponse getMapping(String... indices) {
        final GetMappingsRequest request = new GetMappingsRequest().indices(indices);
        return client.execute((c, requestOptions) -> c.indices().getMapping(request, requestOptions));
    }

    @Override
    public JsonNode getTemplate(String templateName) {
        final GetIndexTemplatesRequest request = new GetIndexTemplatesRequest(templateName);
        final GetIndexTemplatesResponse result = client.execute((c, requestOptions) -> c.indices().getIndexTemplate(request, requestOptions));
        return null;
    }

    @Override
    public void putTemplate(String templateName, Map<String, Object> source) {
        final PutIndexTemplateRequest request = new PutIndexTemplateRequest(templateName).source(source);
        client.execute((c, requestOptions) -> c.indices().putTemplate(request, requestOptions),
                "Unable to put template " + templateName);
    }

    @Override
    public void deleteTemplates(String... templates) {
        for (String template : templates) {
            final DeleteIndexTemplateRequest deleteIndexTemplateRequest = new DeleteIndexTemplateRequest(template);
            client.execute((c, requestOptions) -> c.indices().deleteTemplate(deleteIndexTemplateRequest, requestOptions));
        }
    }

    @Override
    public void waitForGreenStatus(String... indices) {
        waitForStatus(ClusterHealthStatus.GREEN, indices);
    }

    private void waitForStatus(ClusterHealthStatus status, String... indices) {
        final ClusterHealthRequest clusterHealthRequest = new ClusterHealthRequest(indices);
        clusterHealthRequest.waitForStatus(status);

        client.execute((c, requestOptions) -> c.cluster().health(clusterHealthRequest, requestOptions));
    }

    @Override
    public void refreshNode() {
        final RefreshRequest refreshRequest = new RefreshRequest();
        client.execute((c, requestOptions) -> c.indices().refresh(refreshRequest, requestOptions));
    }

    @Override
    public void bulkIndex(BulkIndexRequest bulkIndexRequest) {
        final BulkRequest bulkRequest = new BulkRequest();
        bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        bulkIndexRequest.requests()
                .forEach((indexName, documents) -> documents
                        .forEach(doc -> bulkRequest.add(createIndexRequest(indexName, doc))));
        client.execute((c, requestOptions) -> c.bulk(bulkRequest, requestOptions));
    }

    private IndexRequest createIndexRequest(String indexName, Map<String, Object> source) {
        return new IndexRequest(indexName)
                .source(source);
    }

    @Override
    public void cleanUp() {
        deleteIndices(existingIndices());
        deleteTemplates(existingTemplates());
    }

    private String[] existingTemplates() {
        final GetIndexTemplatesRequest getIndexTemplatesRequest = new GetIndexTemplatesRequest("*");
        final GetIndexTemplatesResponse result = client.execute((c, requestOptions) -> c.indices().getIndexTemplate(getIndexTemplatesRequest, requestOptions));
        return result.getIndexTemplates().stream()
                .map(IndexTemplateMetaData::name)
                .toArray(String[]::new);
    }

    private String[] existingIndices() {
        final GetIndexRequest getIndexRequest = new GetIndexRequest("*");
        getIndexRequest.indicesOptions(IndicesOptions.lenientExpandOpen());
        final GetIndexResponse result = client.execute((c, requestOptions) -> c.indices().get(getIndexRequest, requestOptions));
        return result.getIndices();
    }

    @Override
    public void putSetting(String setting, String value) {
        final ClusterUpdateSettingsRequest request = new ClusterUpdateSettingsRequest();

        request.persistentSettings(Settings.builder().put(setting, value));

        client.execute((c, requestOptions) -> c.cluster().putSettings(request, requestOptions),
                "Unable to update ES cluster setting: " + setting + "=" + value);
    }

    @Override
    public void waitForIndexBlock(String index) {
        waitForResult(() -> indexBlocksPresent(index));
    }

    @Override
    public void resetIndexBlock(String index) {
        final UpdateSettingsRequest request = new UpdateSettingsRequest(index)
                .settings(Collections.singletonMap(
                        "index.blocks.read_only_allow_delete", null
                ));

        client.execute((c, requestOptions) -> c.indices().putSettings(request, requestOptions),
                "Unable to reset index block for " + index);
    }

    private void waitForResult(Callable<Boolean> task) {
        final Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
                .retryIfResult(result -> result == null || !result)
                .withStopStrategy(StopStrategies.stopAfterDelay(1, TimeUnit.MINUTES))
                .withWaitStrategy(WaitStrategies.fixedWait(1, TimeUnit.SECONDS))
                .build();

        try {
            retryer.call(task);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean indexBlocksPresent(String index) {
        final Response result = client.execute((c, requestOptions) -> c.getLowLevelClient()
                .performRequest(new Request("GET", "/" + index)),
                "Unable to retrieve index blocks for index " + index);

        final JsonNode jsonResult;
        try {
            jsonResult = objectMapper.readTree(result.getEntity().getContent());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return jsonResult.path(index)
                .path("settings")
                .path("index")
                .path("blocks")
                .fields()
                .hasNext();
    }
}
