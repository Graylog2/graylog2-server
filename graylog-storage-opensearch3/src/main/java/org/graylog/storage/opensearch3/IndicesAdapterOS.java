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
package org.graylog.storage.opensearch3;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.joschi.jadconfig.util.Duration;
import jakarta.inject.Inject;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.EnumUtils;
import org.graylog.storage.opensearch3.blocks.BlockSettingsParser;
import org.graylog.storage.opensearch3.cluster.ClusterStateApi;
import org.graylog.storage.opensearch3.indextemplates.OSSerializationUtils;
import org.graylog.storage.opensearch3.stats.ClusterStatsApi;
import org.graylog.storage.opensearch3.stats.IndexStatisticsBuilder;
import org.graylog.storage.opensearch3.stats.StatsApi;
import org.graylog2.datatiering.WarmIndexInfo;
import org.graylog2.indexer.IndexNotFoundException;
import org.graylog2.indexer.indices.HealthStatus;
import org.graylog2.indexer.indices.IndexMoveResult;
import org.graylog2.indexer.indices.IndexSettings;
import org.graylog2.indexer.indices.IndexStatus;
import org.graylog2.indexer.indices.IndexTemplateAdapter;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.indices.IndicesAdapter;
import org.graylog2.indexer.indices.ShardsInfo;
import org.graylog2.indexer.indices.Template;
import org.graylog2.indexer.indices.blocks.IndicesBlockStatus;
import org.graylog2.indexer.indices.stats.IndexStatistics;
import org.graylog2.indexer.searches.IndexRangeStats;
import org.graylog2.plugin.Message;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetStats;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.ExpandWildcard;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.FilterAggregate;
import org.opensearch.client.opensearch._types.aggregations.MaxAggregate;
import org.opensearch.client.opensearch._types.aggregations.MinAggregate;
import org.opensearch.client.opensearch._types.aggregations.StringTermsAggregate;
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch.cat.IndicesRequest;
import org.opensearch.client.opensearch.cat.IndicesResponse;
import org.opensearch.client.opensearch.cat.OpenSearchCatClient;
import org.opensearch.client.opensearch.cat.indices.IndicesRecord;
import org.opensearch.client.opensearch.core.ReindexResponse;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.reindex.Destination;
import org.opensearch.client.opensearch.core.reindex.Source;
import org.opensearch.client.opensearch.generic.Body;
import org.opensearch.client.opensearch.generic.Request;
import org.opensearch.client.opensearch.generic.Requests;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.ForcemergeRequest;
import org.opensearch.client.opensearch.indices.GetAliasRequest;
import org.opensearch.client.opensearch.indices.GetAliasResponse;
import org.opensearch.client.opensearch.indices.GetIndexRequest;
import org.opensearch.client.opensearch.indices.GetIndexResponse;
import org.opensearch.client.opensearch.indices.GetIndicesSettingsRequest;
import org.opensearch.client.opensearch.indices.GetIndicesSettingsResponse;
import org.opensearch.client.opensearch.indices.GetMappingResponse;
import org.opensearch.client.opensearch.indices.IndexState;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;
import org.opensearch.client.opensearch.indices.PutIndicesSettingsRequest;
import org.opensearch.client.opensearch.indices.RefreshRequest;
import org.opensearch.client.opensearch.indices.get_mapping.IndexMappingRecord;
import org.opensearch.client.opensearch.indices.stats.IndicesStats;
import org.opensearch.client.opensearch.indices.update_aliases.AddAction;
import org.opensearch.client.opensearch.indices.update_aliases.RemoveAction;
import org.opensearch.client.transport.httpclient5.ResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.ObjectUtils.getIfNull;
import static org.graylog.storage.opensearch3.OfficialOpensearchClient.mapException;

public class IndicesAdapterOS implements IndicesAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(IndicesAdapterOS.class);
    private final OfficialOpensearchClient c;
    private final StatsApi statsApi;
    private final ClusterStatsApi clusterStatsApi;
    private final ClusterStateApi clusterStateApi;
    private final IndexTemplateAdapter indexTemplateAdapter;
    private final IndexStatisticsBuilder indexStatisticsBuilder;
    private final ObjectMapper objectMapper;

    private final org.opensearch.client.opensearch.OpenSearchClient openSearchClient;
    private final OpenSearchIndicesClient indicesClient;
    private final OpenSearchCatClient catClient;
    private final PlainJsonApi jsonApi;
    private final OSSerializationUtils osSerializationUtils;

    // this is the maximum amount of bytes that the index list is supposed to fill in a request,
    // it assumes that these don't need url encoding. If we exceed the maximum, we request settings for all indices
    // and filter after wards
    private static final int MAX_INDICES_URL_LENGTH = 3000;

    @Inject
    public IndicesAdapterOS(OfficialOpensearchClient c,
                            StatsApi statsApi,
                            ClusterStatsApi clusterStatsApi,
                            ClusterStateApi clusterStateApi,
                            IndexTemplateAdapter indexTemplateAdapter,
                            IndexStatisticsBuilder indexStatisticsBuilder,
                            ObjectMapper objectMapper,
                            PlainJsonApi jsonApi,
                            final OSSerializationUtils osSerializationUtils) {
        this.c = c;
        this.statsApi = statsApi;
        this.clusterStatsApi = clusterStatsApi;
        this.clusterStateApi = clusterStateApi;
        this.indexTemplateAdapter = indexTemplateAdapter;
        this.indexStatisticsBuilder = indexStatisticsBuilder;
        this.objectMapper = objectMapper;
        this.openSearchClient = c.sync();
        this.jsonApi = jsonApi;
        this.indicesClient = openSearchClient.indices();
        this.catClient = openSearchClient.cat();
        this.osSerializationUtils = osSerializationUtils;
    }

    @Override
    public void move(String source, String target, Consumer<IndexMoveResult> resultCallback) {
        ReindexResponse result = c.execute(() -> openSearchClient.reindex(
                        org.opensearch.client.opensearch.core.ReindexRequest.builder()
                                .source(Source.builder().index(source).build())
                                .dest(Destination.builder().index(target).build())
                                .build()),
                "Error moving index " + source + " to " + target);

        final IndexMoveResult indexMoveResult = IndexMoveResult.create(
                Math.toIntExact(getIfNull(result.total(), 0L)),
                getIfNull(result.took(), 0L),
                !result.failures().isEmpty()
        );
        resultCallback.accept(indexMoveResult);
    }

    @Override
    public void delete(String index) {
        c.execute(() -> indicesClient.delete(
                        org.opensearch.client.opensearch.indices.DeleteIndexRequest.builder()
                                .index(index)
                                .build()),
                "Error removing index " + index);
    }

    @Override
    public Set<String> resolveAlias(String alias) {
        if (!aliasExists(alias)) { // needed to ensure same behavior as old client
            return Collections.emptySet();
        }
        GetAliasResponse result = c.execute(() -> indicesClient.getAlias(GetAliasRequest.builder()
                .name(alias)
                .build()), "Error resolving alias " + alias);
        return result.result().keySet();
    }

    @Override
    public void create(String index, IndexSettings indexSettings) {
        executeCreateIndexRequest(index, createIndexRequest(index, indexSettings, null));
    }

    @Override
    public void create(String index, IndexSettings indexSettings, @Nullable Map<String, Object> mapping) {
        executeCreateIndexRequest(index, createIndexRequest(index, indexSettings, mapping));
    }

    private CreateIndexRequest createIndexRequest(String index,
                                                  IndexSettings indexSettings,
                                                  @Nullable Map<String, Object> mapping) {

        CreateIndexRequest.Builder builder = new CreateIndexRequest.Builder()
                .index(index)
                .settings(createIndexSettings(indexSettings));
        if (mapping != null) {
            try {
                builder.mappings(osSerializationUtils.fromMap(mapping, TypeMapping._DESERIALIZER));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return builder.build();
    }

    private org.opensearch.client.opensearch.indices.IndexSettings createIndexSettings(IndexSettings indexSettings) {

        Map<String, JsonData> jsonSettings = indexSettings.map().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> JsonData.of(entry.getValue())
                ));

        org.opensearch.client.opensearch.indices.IndexSettings.Builder builder = org.opensearch.client.opensearch.indices.IndexSettings.builder();
        builder.customSettings(jsonSettings);
        return builder.build();
    }


    private void executeCreateIndexRequest(String index, CreateIndexRequest request) {
        c.execute(() -> indicesClient.create(request), "Unable to create index " + index);
    }

    @Override
    public void updateIndexMapping(@Nonnull String indexName,
                                   @Nonnull String mappingType,
                                   @Nonnull Map<String, Object> mapping) {
        try {
            Request request = org.opensearch.client.opensearch.generic.Requests.builder()
                    .endpoint("/" + indexName + "/_mapping")
                    .method("PUT")
                    .body(Body.from(objectMapper.writeValueAsBytes(mapping), "application/json"))
                    .build();
            jsonApi.performRequest(request, "Unable to update index mapping " + indexName);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to update index mapping " + indexName, e);
        }
    }

    @Override
    public Map<String, Object> getIndexMapping(@Nonnull String index) {
        return c.execute(() -> {
            GetMappingResponse response = indicesClient.getMapping(r -> r.index(index)
                    .allowNoIndices(true)
                    .ignoreUnavailable(true)
                    .expandWildcards(ExpandWildcard.All));
            IndexMappingRecord indexMappingRecord = response.get(index);
            return osSerializationUtils.toMap(indexMappingRecord.mappings());
        }, "Couldn't read mapping of index " + index);
    }

    @Override
    public Map<String, Object> getStructuredIndexSettings(@Nonnull String index) {
        Request request = Requests.builder()
                .method("GET")
                .endpoint("/" + index + "/_settings")
                .build();
        JsonNode jsonNode = jsonApi.performRequest(request, "Unable to retrieve index settings " + index);
        return Optional.ofNullable(jsonNode.get(index))
                .map(node -> node.get("settings"))
                .map(node -> objectMapper.convertValue(node, new TypeReference<Map<String, Object>>() {}))
                .orElse(Map.of());
    }

    /**
     * returns a map of flat (i.e. with dot notation) index settings
     */
    private Map<String, Object> getFlatIndexSettings(String index) {
        return c.execute(() -> {
            GetIndicesSettingsResponse result = indicesClient.getSettings(b -> b.index(index)
                    .ignoreUnavailable(true)
                    .allowNoIndices(true)
                    .expandWildcards(ExpandWildcard.Open)
                    .flatSettings(true)
            );
            return toIndexSettings(result, index);
        }, "Couldn't read settings for index " + index);
    }

    /**
     * serializes a flat(!) GetIndicesSettingsResponse into a HashMap
     *
     * @param response GetIndicesSettingsResponse (Attention: use flatSettings param for retrieval)
     * @param index    index to get settings for
     * @return Map of settings
     */
    public static Map<String, Object> toIndexSettings(GetIndicesSettingsResponse response, String index) {
        IndexState r = response.get(index);
        if (r == null) {
            LOG.warn("Couldn't read settings for index {}", index);
            return null;
        }
        org.opensearch.client.opensearch.indices.IndexSettings indexSettings = r.settings();
        if (indexSettings == null) {
            LOG.warn("Couldn't read settings for index {}", index);
            return null;
        }
        Map<String, Object> settings;
        try {
            settings = new ObjectMapperProvider().get().readValue(indexSettings.toJsonString(), new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            LOG.warn("Couldn't read settings for index {}", index);
            return null;
        }
        return settings.entrySet().stream().collect(
                Collectors.toMap(
                        (entry) -> (entry.getKey().startsWith("index.") ? entry.getKey() : "index." + entry.getKey()),
                        Map.Entry::getValue));
    }

    @Override
    public void updateIndexMetaData(@Nonnull String index, @Nonnull Map<String, Object> metadata, boolean mergeExisting) {
        Map<String, Object> metaUpdate = new HashMap<>();
        if (mergeExisting) {
            final Map<String, Object> oldMetaData = getIndexMetaData(index);
            metaUpdate.putAll(oldMetaData);
        }
        metaUpdate.putAll(metadata);
        updateIndexMapping(index, "ignored", Map.of("_meta", metaUpdate));
    }

    @Override
    public Map<String, Object> getIndexMetaData(@Nonnull String index) {
        final Object metaData = getIndexMapping(index).get("_meta");
        //noinspection rawtypes
        if (metaData instanceof Map map) {
            //noinspection unchecked
            return map;
        }
        return Map.of();
    }

    @Override
    public boolean ensureIndexTemplate(String templateName, Template template) {
        return indexTemplateAdapter.ensureIndexTemplate(templateName, template);
    }

    @Override
    public boolean indexTemplateExists(String templateName) {
        return indexTemplateAdapter.indexTemplateExists(templateName);
    }

    @Override
    public boolean deleteIndexTemplate(String templateName) {
        return indexTemplateAdapter.deleteIndexTemplate(templateName);
    }

    @Override
    public Optional<DateTime> indexCreationDate(String index) {
        Map<String, Object> settings = getFlatIndexSettings(index);
        Optional<String> creationDate = Optional.ofNullable(settings)
                .map(s -> s.get("index.creation_date"))
                .map(String::valueOf);
        return creationDate
                .map(Long::valueOf)
                .map(instant -> new DateTime(instant, DateTimeZone.UTC));
    }

    @Override
    public Optional<DateTime> indexClosingDate(String index) {
        final Map<String, Object> indexMetaData = getIndexMetaData(index);
        return Optional.ofNullable(indexMetaData.get("closing_date")).filter(Long.class::isInstance).map(Long.class::cast)
                .map(instant -> new DateTime(instant, DateTimeZone.UTC));
    }

    @Override
    public void openIndex(String index) {
        c.execute(() -> indicesClient.open(r -> r.index(index)), "Couldn't open index " + index);
    }

    @Override
    public void setReadOnly(String index) {
        // https://www.elastic.co/guide/en/elasticsearch/reference/7.8/indices-update-settings.html
        // https://www.elastic.co/guide/en/elasticsearch/reference/7.10/index-modules-blocks.html
        PutIndicesSettingsRequest request = PutIndicesSettingsRequest.of(r -> r
                .index(index)
                .settings(s -> s
                        .blocksWrite(true)
                        .blocksRead(false)
                        .blocksMetadata(false)
                )
        );

        c.execute(() -> indicesClient.putSettings(request), "Couldn't set index " + index + " to read-only");
    }

    @Override
    public void flush(String index) {
        c.execute(() -> indicesClient.flush(r -> r.index(index)), "Unable to flush index " + index);
    }

    @Override
    public void markIndexReopened(String index) {
        final String aliasName = index + Indices.REOPENED_ALIAS_SUFFIX;
        c.execute(() -> indicesClient.updateAliases(request ->
                request.actions(action ->
                        action.add(addAction ->
                                addAction.index(index).alias(aliasName)
                        )
                )
        ), "Couldn't create reopened alias for index " + index);
    }

    @Override
    public void removeAlias(String index, String alias) {
        removeAliases(Set.of(index), alias);
    }

    @Override
    public void close(String index) {
        c.execute(() -> indicesClient.close(r -> r.index(index)), "Unable to close index " + index);
    }

    @Override
    public Map<String, Set<String>> aliases(String indexPattern) {
        GetAliasRequest request = GetAliasRequest.of(r -> r
                .index(indexPattern)
                .ignoreUnavailable(false)
                .allowNoIndices(true)
                .expandWildcards(ExpandWildcard.Open)
        );
        return c.execute(() -> {
            GetAliasResponse aliases = indicesClient.getAlias(request);
            return aliases.result().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey,
                            entry -> entry.getValue().aliases().keySet()
                    ));
        }, "Couldn't collect aliases for index pattern " + indexPattern);
    }

    @Override
    public Map<String, Set<String>> fieldsInIndices(String[] writeIndexWildcards) {
        final List<String> indexWildCards = Arrays.asList(writeIndexWildcards);
        return clusterStateApi.fields(indexWildCards);
    }

    @Override
    public Set<String> closedIndices(Collection<String> indices) {
        return c.execute(() -> {
            List<IndicesRecord> indicesRecords = catClient.indices(r -> r.index(indices.stream().toList())
                            .expandWildcards(ExpandWildcard.Closed))
                    .valueBody();
            return indicesRecords.stream()
                    .filter(i -> Objects.nonNull(i.status()))
                    .filter(i -> i.status().equals("close"))
                    .map(IndicesRecord::index)
                    .collect(Collectors.toSet());
        }, "Unable to retrieve list of closed indices for " + indices);
    }

    @Override
    public Set<IndexStatistics> indicesStats(final Collection<String> indices) {
        return statsApi.indicesStatsWithShardLevel(indices).entrySet()
                .stream()
                .map(entry -> {
                    final String index = entry.getKey();
                    final IndicesStats indexStats = entry.getValue();
                    if (indexStats != null) {
                        return indexStatisticsBuilder.build(index, indexStats);
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<IndexStatistics> getIndexStats(final String index) {
        final IndicesStats indicesStats = statsApi.indexStatsWithShardLevel(index);
        return indicesStats == null
                ? Optional.empty()
                : Optional.of(indexStatisticsBuilder.build(index, indicesStats));
    }

    @Override
    public JsonNode getIndexStats(Collection<String> indices) {

        try {
            final Map<String, IndicesStats> stringIndicesStatsMap = statsApi.indicesStatsWithDocsAndStore(indices);
            ObjectNode node = objectMapper.createObjectNode();
            for (Map.Entry<String, IndicesStats> entry : stringIndicesStatsMap.entrySet()) {
                final JsonNode statsNode = objectMapper.readTree(entry.getValue().toJsonString());
                node.set(entry.getKey(), statsNode);
            }
            return node;
        } catch (JsonProcessingException e) {
            LOG.error("Unable to convert indices stats to JSON", e);
            return null;
        }

    }

    @Override
    public IndexSetStats getIndexSetStats() {
        return clusterStatsApi.clusterStats();
    }

    @Override
    public List<ShardsInfo> getShardsInfo(String indexName) {
        return c.execute(() -> catClient.shards(r -> r.index(indexName)).valueBody().stream()
                .map(shardsRecord -> new ShardsInfo(
                        shardsRecord.index(), Integer.parseInt(getIfNull(shardsRecord.shard(), "0")),
                        ShardsInfo.ShardType.fromString(getIfNull(shardsRecord.prirep(), "UNKNOWN")),
                        EnumUtils.getEnumIgnoreCase(ShardsInfo.State.class, shardsRecord.state(), ShardsInfo.State.UNKNOWN),
                        Long.parseLong(getIfNull(shardsRecord.docs(), "0")), shardsRecord.store(),
                        shardsRecord.ip(), shardsRecord.node())
                ).toList(), "Error getting shards information for " + indexName);
    }


    @Override
    public IndicesBlockStatus getIndicesBlocksStatus(final List<String> indices) {
        if (indices == null || indices.isEmpty()) {
            throw new IllegalArgumentException("Expecting list of indices with at least one index present.");
        }
        final var maxLengthExceeded = String.join(",", indices).length() > MAX_INDICES_URL_LENGTH;
        final List<String> safeIndices = maxLengthExceeded ? new ArrayList<>() : indices;

        GetIndicesSettingsRequest request = GetIndicesSettingsRequest.of(r -> r
                .ignoreUnavailable(false)
                .allowNoIndices(true)
                .expandWildcards(ExpandWildcard.Open, ExpandWildcard.Closed)
                .flatSettings(true)
                .name("index.blocks.read", "index.blocks.write", "index.blocks.metadata", "index.blocks.read_only", "index.blocks.read_only_allow_delete")
                .index(safeIndices)
        );

        return c.execute(() -> {
            GetIndicesSettingsResponse settingsResponse = indicesClient.getSettings(request);
            return BlockSettingsParser.parseBlockSettings(settingsResponse, maxLengthExceeded ? Optional.of(indices) : Optional.empty());
        }, "Error getting indices settings for " + indices);
    }

    @Override
    public boolean exists(String index) {
        try {
            GetIndicesSettingsResponse result = indicesClient.getSettings(b -> b.index(index)
                    .ignoreUnavailable(true)
                    .allowNoIndices(true)
                    .expandWildcards(ExpandWildcard.Open)
                    .flatSettings(true)
            );
            return result.result() != null && result.result().size() == 1 && result.result().containsKey(index);
        } catch (IOException | OpenSearchException e) {
            if (e instanceof OpenSearchException && e.getMessage().contains("no such index")) {
                return false;
            }
            throw mapException(e, "Unable to determine if index exists " + index);
        }
    }

    @Override
    public boolean aliasExists(String alias) {
        return c.execute(() -> indicesClient.existsAlias(r -> r.name(alias)).value(),
                "Error trying to check if alias exists: " + alias);
    }

    @Override
    public Set<String> indices(String indexWildcard, List<IndexStatus> status, String indexSetId) {
        final List<ExpandWildcard> ewc = (status.isEmpty()) ? List.of(ExpandWildcard.All) : status.stream().map(this::statusToEwc).toList();
        return c.execute(() -> {
            GetIndexResponse result = indicesClient.get(GetIndexRequest.of(b -> b
                    .index(indexWildcard)
                    .flatSettings(true)
                    .expandWildcards(ewc)
                    .ignoreUnavailable(true)));
            return result.result().keySet();
        }, "Couldn't get index list for index set <" + indexSetId + ">");
    }

    private ExpandWildcard statusToEwc(IndexStatus status) {
        return switch (status) {
            case OPEN -> ExpandWildcard.Open;
            case CLOSED -> ExpandWildcard.Closed;
        };
    }

    @Override
    public Optional<Long> storeSizeInBytes(String index) {
        return statsApi.storeSizes(index);
    }

    @Override
    public void cycleAlias(String aliasName, String targetIndex) {
        c.execute(() -> indicesClient.updateAliases(request ->
                request.actions(action ->
                        action.add(addAction ->
                                addAction.index(targetIndex).alias(aliasName)
                        )
                )
        ), "Couldn't point alias " + aliasName + " to index " + targetIndex);
    }

    @Override
    public void cycleAlias(String aliasName, String targetIndex, String oldIndex) {
        c.execute(() -> indicesClient.updateAliases(request ->
                request.actions(
                        new RemoveAction.Builder().index(oldIndex).alias(aliasName).build().toAction(),
                        new AddAction.Builder().index(targetIndex).alias(aliasName).build().toAction()
                )
        ), "Couldn't switch alias " + aliasName + " from index " + oldIndex + " to index " + targetIndex);
    }

    @Override
    public void removeAliases(Set<String> indices, String alias) {
        c.execute(() -> indicesClient.updateAliases(request ->
                request.actions(action ->
                        action.remove(removeAction ->
                                removeAction.indices(indices.stream().toList()).alias(alias)
                        )
                )
        ), "Couldn't remove alias " + alias + " from indices " + indices);
    }

    @Override
    public void optimizeIndex(String index, int maxNumSegments, Duration timeout) {
        ForcemergeRequest request = ForcemergeRequest.of(b -> b
                .index(index)
                .maxNumSegments(Integer.toUnsignedLong(maxNumSegments))
                .flush(true)
        );

        String errorMessage = "Force merge of index " + index + " did not complete in " + timeout.toString() + ", not waiting for completion any longer.";
        c.executeWithClientTimeout((asyncClient) -> asyncClient.indices().forcemerge(request), errorMessage, timeout);

    }

    @Override
    public IndexRangeStats indexRangeStatsOfIndex(String index) {

        Aggregation aggregation = Aggregation.builder()
                .filter(f -> f.exists(e -> e.field(Message.FIELD_TIMESTAMP)))
                .aggregations(Map.of(
                        "ts_min", Aggregation.of(b -> b.min(m -> m.field(Message.FIELD_TIMESTAMP))),
                        "ts_max", Aggregation.of(b -> b.max(m -> m.field(Message.FIELD_TIMESTAMP))),
                        "streams", Aggregation.of(b -> b.terms(t -> t.field(Message.FIELD_STREAMS).size(Integer.MAX_VALUE)))
                ))
                .build();

        SearchRequest request = SearchRequest.of(r -> r
                .index(index)
                .aggregations("agg", aggregation)
                .searchType(org.opensearch.client.opensearch._types.SearchType.DfsQueryThenFetch)
                .ignoreUnavailable(false)
                .allowNoIndices(true)
                .expandWildcards(ExpandWildcard.All)
        );

        org.opensearch.client.opensearch.core.SearchResponse<Void> result = c.execute(() -> c.sync().search(request, Void.class),
                "Couldn't build index range of index " + index);

        if (result.shards().total() == 0 || result.aggregations() == null) {
            throw new IndexNotFoundException("Couldn't build index range of index " + index + " because it doesn't exist.");
        }
        final FilterAggregate f = result.aggregations().get("agg").filter();
        if (f == null) {
            throw new IndexNotFoundException("Couldn't build index range of index " + index + " because it doesn't exist.");
        } else if (f.docCount() == 0L) {
            LOG.debug("No documents with attribute \"timestamp\" found in index <{}>", index);
            return IndexRangeStats.EMPTY;
        }

        final MinAggregate minAgg = f.aggregations().get("ts_min").min();
        final long minUnixTime = (minAgg.value() == null) ? 0 : minAgg.value().longValue();
        final DateTime min = new DateTime(minUnixTime, DateTimeZone.UTC);
        final MaxAggregate maxAgg = f.aggregations().get("ts_max").max();
        final long maxUnixTime = (maxAgg.value() == null) ? 0 : maxAgg.value().longValue();
        final DateTime max = new DateTime(maxUnixTime, DateTimeZone.UTC);
        // make sure we return an empty list, so we can differentiate between old indices that don't have this information
        // and newer ones that simply have no streams.
        final StringTermsAggregate streams = f.aggregations().get("streams").sterms();
        final List<String> streamIds = streams.buckets().array().stream()
                .map(StringTermsBucket::key)
                .collect(toList());

        return IndexRangeStats.create(min, max, streamIds);
    }

    @Override
    public HealthStatus waitForRecovery(String index) {
        return waitForRecovery(index, 30);
    }

    @Override
    public HealthStatus waitForRecovery(String index, int timeout) {
        try {
            return c.execute(() ->
                    HealthStatus.fromString(c.sync().cluster().health(r -> r
                            .index(index)
                            .timeout(t -> t.time(timeout + "s"))
                            .waitForStatus(org.opensearch.client.opensearch._types.HealthStatus.Green)
                    ).status().jsonValue()), "Error waiting for index recovery");
        } catch (RuntimeException e) {
            if (e.getCause() instanceof ResponseException ex) {
                String status = StringUtils.substringBetween(ex.getMessage(), "\"status\":\"", "\"");
                return HealthStatus.fromString(status);
            }
            throw e;
        }
    }

    @Override
    public boolean isOpen(String index) {
        return indexHasState(index, State.Open);
    }

    @Override
    public boolean isClosed(String index) {
        return indexHasState(index, State.Closed);
    }

    @Override
    public void refresh(String... indices) {
        c.execute(() -> indicesClient.refresh(RefreshRequest.of(r -> r.index(Arrays.stream(indices).toList()))),
                "Unable to refresh indices " + Arrays.toString(indices));
    }

    private Boolean indexHasState(String index, State open) {
        return indexState(index)
                .map(state -> state.equals(open))
                .orElseThrow(() -> new IndexNotFoundException("Unable to determine state for absent index " + index));
    }

    private Optional<State> indexState(String index) {
        return c.execute(() -> {
            IndicesResponse indices = catClient.indices(IndicesRequest.of(b -> b.index(index).headers("index", "status")));
            return indices.valueBody().stream()
                    .map(IndicesRecord::status)
                    .filter(Objects::nonNull)
                    .map(State::parse)
                    .findFirst();
        }, "Unable to retrieve index stats for " + index);
    }

    enum State {
        Open,
        Closed;

        static State parse(String state) {
            return switch (state.toLowerCase(Locale.ENGLISH)) {
                case "open" -> Open;
                case "close" -> Closed;
                default -> throw new IllegalStateException("Unable to parse invalid index state: " + state);
            };

        }
    }

    @Override
    public String getIndexId(String index) {
        return getFlatIndexSettings(index).get("index.uuid").toString();
    }

    @Override
    public Optional<WarmIndexInfo> getWarmIndexInfo(String index) {
        return Optional.ofNullable(getFlatIndexSettings(index))
                .filter(settings -> "remote_snapshot".equals(settings.get("index.store.type")))
                .map(settings -> mapIndexSettingsToSearchableSnapshot(index, settings));
    }

    private WarmIndexInfo mapIndexSettingsToSearchableSnapshot(String index, Map<String, Object> settings) {
        String initialIndexName = settings.get("index.provided_name").toString();
        String repository = settings.get("index.searchable_snapshot.repository").toString();
        String snapshotName = settings.get("index.searchable_snapshot.snapshot_id.name").toString();

        return new WarmIndexInfo(index, initialIndexName, repository, snapshotName);
    }
}
