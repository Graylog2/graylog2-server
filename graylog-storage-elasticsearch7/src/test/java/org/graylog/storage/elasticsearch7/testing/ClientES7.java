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
package org.graylog.storage.elasticsearch7.testing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.collect.Streams;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.cluster.settings.ClusterGetSettingsRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.cluster.settings.ClusterGetSettingsResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.cluster.settings.ClusterUpdateSettingsRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest.AliasActions;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.indices.template.delete.DeleteIndexTemplateRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.bulk.BulkRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.index.IndexRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.support.WriteRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.Request;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.Response;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.CloseIndexRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.CreateIndexRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.DeleteComposableIndexTemplateRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.GetComposableIndexTemplateRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.GetIndexRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.GetIndexTemplatesRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.GetMappingsRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.GetMappingsResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.IndexTemplateMetadata;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.PutComposableIndexTemplateRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.PutIndexTemplateRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.PutMappingRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.cluster.metadata.ComposableIndexTemplate;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.common.compress.CompressedXContent;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.common.settings.Settings;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.common.xcontent.XContentType;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.QueryBuilders;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.SearchHit;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog.storage.elasticsearch7.ElasticsearchClient;
import org.graylog.testing.elasticsearch.BulkIndexRequest;
import org.graylog.testing.elasticsearch.Client;
import org.graylog2.indexer.indices.Template;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.graylog2.indexer.Constants.COMPOSABLE_INDEX_TEMPLATES_FEATURE;

public class ClientES7 implements Client {
    private static final Logger LOG = LoggerFactory.getLogger(ClientES7.class);
    private final ElasticsearchClient client;
    private final List<String> featureFlags;
    private final ObjectMapper objectMapper;

    public ClientES7(final ElasticsearchClient client, final List<String> featureFlags) {
        this.client = client;
        this.featureFlags = featureFlags;
        this.objectMapper = new ObjectMapperProvider().get();
    }

    public Optional<Map<String, Object>> findMessage(String index, String query) {
        return client.execute((restHighLevelClient, requestOptions) -> {
            final SearchRequest searchRequest = new SearchRequest();
            searchRequest.indices(index);
            final SearchSourceBuilder source = new SearchSourceBuilder();
            source.query(QueryBuilders.queryStringQuery(query));
            source.size(1);
            searchRequest.source(source);
            final SearchResponse response = restHighLevelClient.search(searchRequest, requestOptions);
            return Arrays.stream(response.getHits().getHits()).map(SearchHit::getSourceAsMap).findFirst();
        });
    }

    @Override
    public void createIndex(String index, int shards, int replicas) {
        LOG.debug("Creating index " + index);
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
        for (String index : indices) {
            if (indicesExists(index)) {
                final DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(index);
                client.execute((c, requestOptions) -> c.indices().delete(deleteIndexRequest, requestOptions));
            }
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
    public void removeAliasMapping(String indexName, String alias) {
        final IndicesAliasesRequest indicesAliasesRequest = new IndicesAliasesRequest();
        final AliasActions aliasAction = new AliasActions(AliasActions.Type.REMOVE)
                .index(indexName)
                .alias(alias);
        indicesAliasesRequest.addAliasAction(aliasAction);

        client.execute((c, requestOptions) -> c.indices().updateAliases(indicesAliasesRequest, requestOptions),
                "failed to remove alias " + alias + " for index " + indexName);
    }

    @Override
    public String fieldType(String testIndexName, String field) {
        return getFieldMappings(testIndexName).get(field);
    }

    private Map<String, String> getFieldMappings(String index) {
        final Request request = new Request("GET", "/" + index + "/_mapping");
        final JsonNode response = client.execute((c, requestOptions) -> {
            request.setOptions(requestOptions);
            final Response result = c.getLowLevelClient().performRequest(request);
            return objectMapper.readTree(result.getEntity().getContent());
        });

        return extractFieldMappings(index, response)
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().path("type").asText()));
    }

    private Stream<Map.Entry<String, JsonNode>> extractFieldMappings(String index, JsonNode response) {
        //noinspection UnstableApiUsage
        return Streams.stream(
                response.path(index)
                        .path("mappings")
                        .path("properties")
                        .fields()
        );
    }

    private boolean composableTemplateExists(String templateName) {
        var request = new GetComposableIndexTemplateRequest("*");
        var result = client.execute((c, requestOptions) -> c.indices().getIndexTemplate(request, requestOptions));
        return result.getIndexTemplates()
                .keySet()
                .stream()
                .anyMatch(indexTemplate -> indexTemplate.equals(templateName));
    }

    private boolean legacyTemplateExists(String templateName) {
        var request = new GetIndexTemplatesRequest("*");
        var result = client.execute((c, requestOptions) -> c.indices().getIndexTemplate(request, requestOptions));
        return result.getIndexTemplates()
                .stream()
                .anyMatch(indexTemplate -> indexTemplate.name().equals(templateName));
    }

    @Override
    public boolean templateExists(String templateName) {
        return featureFlags.contains(COMPOSABLE_INDEX_TEMPLATES_FEATURE) ? composableTemplateExists(templateName) : legacyTemplateExists(templateName);
    }

    private void putComposableTemplate(String templateName, Template template) {
        var serializedMapping = serialize(template.mappings());
        var settings = Settings.builder().loadFromSource(serializeJson(template.settings()), XContentType.JSON).build();
        var esTemplate = new org.graylog.shaded.elasticsearch7.org.elasticsearch.cluster.metadata.Template(settings, serializedMapping, null);
        var indexTemplate = new ComposableIndexTemplate(template.indexPatterns(), esTemplate, null, template.order(), null, null);
        var request = new PutComposableIndexTemplateRequest()
                .name(templateName)
                .indexTemplate(indexTemplate);
        client.execute((c, requestOptions) -> c.indices().putIndexTemplate(request, requestOptions),
                "Unable to put template " + templateName);
    }

    private void putLegacyTemplate(String templateName, Template template) {
        var source = Map.of(
                "index_patterns", template.indexPatterns(),
                "mappings", template.mappings(),
                "settings", template.settings(),
                "order", template.order()
        );
        var request = new PutIndexTemplateRequest(templateName).source(source);
        client.execute((c, requestOptions) -> c.indices().putTemplate(request, requestOptions),
                "Unable to put template " + templateName);
    }

    @Override
    public void putTemplate(String templateName, Template template) {
        if (featureFlags.contains(COMPOSABLE_INDEX_TEMPLATES_FEATURE)) {
            putComposableTemplate(templateName, template);
        } else {
            putLegacyTemplate(templateName, template);
        }
    }

    private String serializeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private CompressedXContent serialize(Object obj) {
        try {
            return new CompressedXContent(serializeJson(obj));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteComposableTemplates(String... templates) {
        for (String template : templates) {
            var deleteIndexTemplateRequest = new DeleteComposableIndexTemplateRequest(template);
            client.execute((c, requestOptions) -> c.indices().deleteIndexTemplate(deleteIndexTemplateRequest, requestOptions));
        }
    }

    private void deleteLegacyTemplates(String... templates) {
        for (String template : templates) {
            var deleteIndexTemplateRequest = new DeleteIndexTemplateRequest(template);
            client.execute((c, requestOptions) -> c.indices().deleteTemplate(deleteIndexTemplateRequest, requestOptions));
        }
    }

    @Override
    public void deleteTemplates(String... templates) {
        if (featureFlags.contains(COMPOSABLE_INDEX_TEMPLATES_FEATURE)) {
            deleteComposableTemplates(templates);
        } else {
            deleteLegacyTemplates(templates);
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
        LOG.debug("Removing indices: " + String.join(",", existingIndices()));
        deleteIndices(existingIndices());
        deleteTemplates(existingTemplates());
        refreshNode();
    }

    private String[] existingComposableTemplates() {
        var getIndexTemplatesRequest = new GetComposableIndexTemplateRequest("");
        var result = client.execute((c, requestOptions) -> c.indices().getIndexTemplate(getIndexTemplatesRequest, requestOptions));
        return result.getIndexTemplates()
                .keySet()
                .toArray(String[]::new);
    }

    private String[] existingLegacyTemplates() {
        var getIndexTemplatesRequest = new GetIndexTemplatesRequest();
        var result = client.execute((c, requestOptions) -> c.indices().getIndexTemplate(getIndexTemplatesRequest, requestOptions));
        return result.getIndexTemplates().stream()
                .map(IndexTemplateMetadata::name)
                .toArray(String[]::new);
    }

    private String[] existingTemplates() {
        return featureFlags.contains(COMPOSABLE_INDEX_TEMPLATES_FEATURE) ? existingComposableTemplates() : existingLegacyTemplates();
    }

    private String[] existingIndices() {
        final Request request = new Request("GET", "/_cat/indices");
        request.addParameter("format", "json");
        request.addParameter("h", "index");

        final JsonNode jsonResponse = client.execute((c, requestOptions) -> {
            request.setOptions(requestOptions);
            final Response response = c.getLowLevelClient().performRequest(request);
            return objectMapper.readTree(response.getEntity().getContent());
        });

        return Streams.stream(jsonResponse.elements())
                .map(index -> index.path("index").asText())
                .distinct()
                .toArray(String[]::new);
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

    @Override
    public void setIndexBlock(String index) {
        final UpdateSettingsRequest request = new UpdateSettingsRequest(index)
                .settings(Collections.singletonMap(
                        "index.blocks.read_only_allow_delete", true
                ));

        client.execute((c, requestOptions) -> c.indices().putSettings(request, requestOptions),
                "Unable to set index block for " + index);
    }

    @Override
    public void updateMapping(String index, Map<String, Object> mapping) {
        final PutMappingRequest request = new PutMappingRequest(index)
                .source(mapping);

        client.execute((c, requestOptions) -> c.indices().putMapping(request, requestOptions),
                "Unable to update index mapping " + index);
    }

    @Override
    public Map<String, Object> getMapping(String index) {
        final GetMappingsRequest request = new GetMappingsRequest().indices(index);

        final GetMappingsResponse result = client.execute((c, requestOptions) -> c.indices().getMapping(request, requestOptions),
                "Couldn't read mapping of index " + index);

        return result.mappings().get(index).sourceAsMap();
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
        final JsonNode jsonResult = client.execute((c, requestOptions) -> {
            final Response result = c.getLowLevelClient()
                    .performRequest(new Request("GET", "/" + index));
            return objectMapper.readTree(result.getEntity().getContent());
        }, "Unable to retrieve index blocks for index " + index);

        return jsonResult.path(index)
                .path("settings")
                .path("index")
                .path("blocks")
                .fields()
                .hasNext();
    }

    public String getClusterSetting(String setting) {
        final ClusterGetSettingsRequest req = new ClusterGetSettingsRequest();
        final ClusterGetSettingsResponse response = client.execute((c, requestOptions) -> c.cluster().getSettings(req, requestOptions),
                "Unable to read ES cluster setting: " + setting);
        return response.getSetting(setting);
    }

    @Override
    public void putFieldMapping(String index, String field, String type) {
        updateMapping(index, Collections.singletonMap("properties", Collections.singletonMap(field, Collections.singletonMap("type", type))));
    }
}
