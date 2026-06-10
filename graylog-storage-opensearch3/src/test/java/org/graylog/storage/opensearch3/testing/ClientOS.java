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
package org.graylog.storage.opensearch3.testing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import jakarta.json.stream.JsonParser;
import org.graylog.storage.opensearch3.OfficialOpensearchClient;
import org.graylog.testing.elasticsearch.BulkIndexRequest;
import org.graylog.testing.elasticsearch.Client;
import org.graylog.testing.elasticsearch.IndexState;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.json.JsonpMapper;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch._types.HealthStatus;
import org.opensearch.client.opensearch._types.Refresh;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch.cat.IndicesResponse;
import org.opensearch.client.opensearch.cat.indices.IndicesRecord;
import org.opensearch.client.opensearch.cluster.GetClusterSettingsResponse;
import org.opensearch.client.opensearch.core.BulkRequest.Builder;
import org.opensearch.client.opensearch.indices.GetIndexResponse;
import org.opensearch.client.opensearch.indices.GetMappingResponse;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.opensearch.client.opensearch.indices.get_mapping.IndexMappingRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.graylog2.indexer.Constants.COMPOSABLE_INDEX_TEMPLATES_FEATURE;

public class ClientOS implements Client {
    private static final Logger LOG = LoggerFactory.getLogger(ClientOS.class);

    private static final Set<String> PROTECTED_INDICES = Set.of(
            ".opensearch-observability",
            ".opendistro_security"
    );

    private final OfficialOpensearchClient opensearchClient;
    private final List<String> featureFlags;
    private final ObjectMapper objectMapper;

    public ClientOS(final OfficialOpensearchClient opensearchClient, final List<String> featureFlags) {
        this.opensearchClient = opensearchClient;
        this.featureFlags = featureFlags;
        this.objectMapper = new ObjectMapperProvider().get();
    }

    @Override
    public void createIndex(String index, int shards, int replicas) {
        LOG.debug("Creating index " + index);
        opensearchClient.sync(c -> c.indices().create(req -> req.index(index).settings(settings -> settings.numberOfShards(shards).numberOfReplicas(replicas))), "Failed to create index " + index);
    }

    @Override
    public void deleteIndices(String... indices) {
        Arrays.stream(indices)
                .filter(i -> !PROTECTED_INDICES.contains(i))
                .forEach(this::deleteIndex);
    }

    private void deleteIndex(String index) {
        opensearchClient.sync(c -> c.indices().delete(r -> r.index(index).ignoreUnavailable(true)), "Failed to delete index " + index);
    }

    public void deleteDataStreams() {
        opensearchClient.sync(c -> c.indices().deleteDataStream(ds -> ds.name("*")), "Failed to delete datastreams");
    }

    @Override
    public void closeIndex(String index) {
        opensearchClient.sync(c -> c.indices().close(req -> req.index(index)), "Failed to close index " + index);
    }

    public Optional<Map<String, Object>> findMessage(String index, String queryString) {
        throw new UnsupportedOperationException("Not supported in os3 client");
    }

    @Override
    public boolean indicesExists(String... indices) {
        return opensearchClient.sync(c -> c.indices().exists(req -> req.index(Arrays.asList(indices))), "Failed to test existence of indices")
                .value();
    }

    @Override
    public void addAliasMapping(String indexName, String alias) {
        opensearchClient.sync(c -> c.indices().putAlias(req -> req.index(indexName).alias(alias)), "failed to add alias " + alias + " for index " + indexName);
    }

    @Override
    public void removeAliasMapping(String indexName, String alias) {
        opensearchClient.sync(c -> c.indices().deleteAlias(req -> req.index(indexName).name(alias)), "failed to remove alias " + alias + " for index " + indexName);
    }

    @Override
    public String fieldType(String testIndexName, String field) {
        return getFieldMappings(testIndexName).get(field);
    }

    private Map<String, String> getFieldMappings(String index) {

        final GetMappingResponse resp = opensearchClient.sync(c -> c.indices().getMapping(r -> r.index(index)), "Failed to get index mapping " + index);
        return resp.get(index)
                .mappings()
                .properties()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, p -> p.getValue()._kind().toString().toLowerCase(Locale.ROOT)));
    }

    private void deleteComposableTemplates() {
        opensearchClient.sync(c -> c.indices().deleteIndexTemplate(req -> req.name("*")), "Failed to delete compostable templates.");
    }

    private void deleteLegacyTemplates() {
        org.opensearch.client.opensearch.generic.Request request = org.opensearch.client.opensearch.generic.Requests.builder()
                .endpoint("/_template/*")
                .method("DELETE")
                .build();
        try {
            final org.opensearch.client.opensearch.generic.Response response = opensearchClient.sync().generic().execute(request);
            if (response.getStatus() != 200) {
                throw new RuntimeException("Failed to delete compostable template s. Reason: " + response.getReason());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void deleteAllTemplates() {
        if (featureFlags.contains(COMPOSABLE_INDEX_TEMPLATES_FEATURE)) {
            deleteComposableTemplates();
        } else {
            deleteLegacyTemplates();
        }
    }

    @Override
    public void waitForGreenStatus(String... indices) {
        opensearchClient.sync(c -> c.cluster().health(req -> req.index(Arrays.asList(indices)).waitForStatus(HealthStatus.Green)), "Failed to wait for cluster health status");
    }

    @Override
    public void refreshNode() {
        opensearchClient.sync(c -> c.indices().refresh(), "Failed to refresh indices");
    }

    @Override
    public void bulkIndex(BulkIndexRequest bulkIndexRequest) {
        Builder bulk = new Builder();
        bulkIndexRequest.requests().forEach((index, docs) -> docs.forEach(doc -> bulk.refresh(Refresh.True).operations(op -> op.index(idx -> idx.index(index).document(doc)))));
        opensearchClient.sync(c -> c.bulk(bulk.build()), "Failed to bulk index documents");
    }

    @Override
    public void cleanUp() {
        LOG.debug("Removing indices: " + String.join(",", existingIndices()));
        deleteDataStreams();
        deleteIndices(existingIndices());
        deleteAllTemplates();
        refreshNode();
    }

    private String[] existingIndices() {
        final IndicesResponse indices = opensearchClient.sync(c -> c.cat().indices(), "Failed to read existing indices");
        return indices.valueBody().stream().map(IndicesRecord::index).distinct().toArray(String[]::new);
    }

    @Override
    public void putSetting(String setting, String value) {
        opensearchClient.sync(c -> c.cluster().putSettings(req -> req.persistent(setting, JsonData.of(value))), "Unable to update OS cluster setting: " + setting + "=" + value);
    }

    @Override
    public void waitForIndexBlock(String index) {
        waitForResult(() -> indexBlocksPresent(index));
    }

    @Override
    public void resetIndexBlock(String index) {
        // TODO: is false enough? Otherwise JsonData.from(JsonValue.NULL)
        opensearchClient.sync(c -> c.indices().putSettings(r -> r.index(index).settings(s -> s.customSettings("index.blocks.read_only_allow_delete", JsonData.of(false)))), "Failed to reset index block " + index);
    }

    @Override
    public void setIndexBlock(String index) {
        opensearchClient.sync(c -> c.indices().putSettings(req -> req.index(index).settings(s -> s.customSettings("index.blocks.read_only_allow_delete", JsonData.of(true)))), "Unable to set index block for " + index);
    }

    @Override
    public void updateMappingMeta(String index, String key, Object value) {
        opensearchClient.sync(c -> c.indices().putMapping(r -> r.index(index).meta(key, JsonData.of(value))), "Failed to update mapping meta");
    }

    @Override
    public <T> T getMappingMetaValue(String index, String key, Class<T> type) {
        final GetMappingResponse mappingResponse = opensearchClient.sync(c -> c.indices().getMapping(r -> r.index(index)), "Failed to et mappings for index " + index);
        final IndexMappingRecord indexMappingRecord = mappingResponse.get(index);
        return indexMappingRecord.mappings().meta().get(key).to(type);
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
        final GetIndexResponse indexResponse = opensearchClient.sync(c -> c.indices().get(req -> req.index(index)), "Failed to get index " + index);
        return Optional.ofNullable(indexResponse.get(index))
                .map(org.opensearch.client.opensearch.indices.IndexState::settings)
                .map(IndexSettings::index)
                .map(IndexSettings::blocks)
                .map(blocks ->
                        Boolean.TRUE.equals(blocks.searchOnly()) ||
                                Boolean.TRUE.equals(blocks.write()) ||
                                Boolean.TRUE.equals(blocks.readOnly()) ||
                                Boolean.TRUE.equals(blocks.readOnlyAllowDelete()) ||
                                Boolean.TRUE.equals(blocks.metadata()) ||
                                Boolean.TRUE.equals(blocks.read()))
                .orElse(false);
    }


    public String getClusterSetting(String setting) {
        final GetClusterSettingsResponse settings = opensearchClient.sync(c -> c.cluster().getSettings(r -> r.flatSettings(true)), "Unable to read OS cluster setting: " + setting);
        return Stream.of(settings.persistent(), settings.transient_(), settings.defaults())
                .map(s -> s.get(setting))
                .filter(Objects::nonNull)
                .findFirst()
                .map(s -> s.to(String.class))
                .orElse(null);
    }

    @Override
    public void putFieldMapping(String index, String field, String type) {
        final Property property = dynamicallyCreateProperty(type);
        opensearchClient.sync(c -> c.indices().putMapping(r -> r.index(index).properties(field, property)), "Failed to update field type");
    }

    /**
     * This is an ugly hack because there is no way to dynamically resolve opensearch property type in os3 client
     *
     */
    private Property dynamicallyCreateProperty(String type) {
        final JsonpMapper mapper = new JacksonJsonpMapper(objectMapper);
        final JsonParser parser = mapper.jsonProvider().createParser(new StringReader("{\"type\": \"" + type + "\"}"));
        return Property._DESERIALIZER.deserialize(parser, mapper);
    }

    @Override
    public IndexState getStatus(String indexName) {
        return opensearchClient.sync(c -> c.cat().indices(req -> req.index(indexName)), "Failed to obtain index status" + indexName)
                .valueBody().stream()
                .map(IndicesRecord::status)
                .filter(Objects::nonNull)
                .map(status -> IndexState.valueOf(status.toUpperCase(Locale.ROOT)))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No index present in cat/indices response"));
    }

    @Override
    public void openIndex(String indexName) {
        opensearchClient.sync(c -> c.indices().open(req -> req.index(indexName)), "Failed to open index " + indexName);
    }
}
