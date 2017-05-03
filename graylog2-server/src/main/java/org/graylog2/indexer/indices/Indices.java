/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.indexer.indices;

import com.github.joschi.jadconfig.util.Duration;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.cluster.Health;
import io.searchbox.cluster.State;
import io.searchbox.core.Bulk;
import io.searchbox.core.BulkResult;
import io.searchbox.core.Cat;
import io.searchbox.core.CatResult;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.SearchScroll;
import io.searchbox.core.search.aggregation.FilterAggregation;
import io.searchbox.core.search.aggregation.MaxAggregation;
import io.searchbox.core.search.aggregation.MinAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import io.searchbox.indices.CloseIndex;
import io.searchbox.indices.CreateIndex;
import io.searchbox.indices.DeleteIndex;
import io.searchbox.indices.Flush;
import io.searchbox.indices.ForceMerge;
import io.searchbox.indices.IndicesExists;
import io.searchbox.indices.OpenIndex;
import io.searchbox.indices.Stats;
import io.searchbox.indices.aliases.AddAliasMapping;
import io.searchbox.indices.aliases.AliasExists;
import io.searchbox.indices.aliases.AliasMapping;
import io.searchbox.indices.aliases.GetAliases;
import io.searchbox.indices.aliases.ModifyAliases;
import io.searchbox.indices.aliases.RemoveAliasMapping;
import io.searchbox.indices.settings.GetSettings;
import io.searchbox.indices.settings.UpdateSettings;
import io.searchbox.indices.template.DeleteTemplate;
import io.searchbox.indices.template.PutTemplate;
import io.searchbox.params.Parameters;
import io.searchbox.params.SearchType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortParseElement;
import org.graylog2.audit.AuditActor;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.indexer.IndexMapping;
import org.graylog2.indexer.IndexNotFoundException;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.cluster.jest.JestUtils;
import org.graylog2.indexer.gson.GsonUtils;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indices.events.IndicesClosedEvent;
import org.graylog2.indexer.indices.events.IndicesDeletedEvent;
import org.graylog2.indexer.indices.events.IndicesReopenedEvent;
import org.graylog2.indexer.indices.stats.IndexStatistics;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.indexer.searches.IndexRangeStats;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.system.NodeId;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.elasticsearch.search.builder.SearchSourceBuilder.searchSource;
import static org.graylog2.audit.AuditEventTypes.ES_INDEX_CREATE;
import static org.graylog2.indexer.gson.GsonUtils.asInteger;
import static org.graylog2.indexer.gson.GsonUtils.asJsonArray;
import static org.graylog2.indexer.gson.GsonUtils.asJsonObject;
import static org.graylog2.indexer.gson.GsonUtils.asLong;
import static org.graylog2.indexer.gson.GsonUtils.asString;

@Singleton
public class Indices {
    private static final Logger LOG = LoggerFactory.getLogger(Indices.class);
    private static final String REOPENED_INDEX_SETTING = "graylog2_reopened";

    private final JestClient jestClient;
    private final Gson gson;
    private final IndexMapping indexMapping;
    private final Messages messages;
    private final NodeId nodeId;
    private final AuditEventSender auditEventSender;
    private final EventBus eventBus;

    @Inject
    public Indices(JestClient jestClient, Gson gson, IndexMapping indexMapping,
                   Messages messages, NodeId nodeId, AuditEventSender auditEventSender,
                   EventBus eventBus) {
        this.jestClient = jestClient;
        this.gson = gson;
        this.indexMapping = indexMapping;
        this.messages = messages;
        this.nodeId = nodeId;
        this.auditEventSender = auditEventSender;
        this.eventBus = eventBus;
    }

    public void move(String source, String target) {
        // TODO: This method should use the Re-index API: https://www.elastic.co/guide/en/elasticsearch/reference/5.3/docs-reindex.html
        final String query = SearchSourceBuilder.searchSource()
                .query(QueryBuilders.matchAllQuery())
                .size(350)
                .sort(SortBuilders.fieldSort(SortParseElement.DOC_FIELD_NAME))
                .toString();

        final Search request = new Search.Builder(query)
                .setParameter(Parameters.SCROLL, "10s")
                .addIndex(source)
                .build();

        final SearchResult searchResult = JestUtils.execute(jestClient, request, () -> "Couldn't process search query response");

        final String scrollId = Optional.of(searchResult.getJsonObject())
                .map(json -> asString(json.get("_scroll_id")))
                .orElseThrow(() -> new ElasticsearchException("Couldn't find scroll ID in search query response"));


        final Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        while (true) {
            final SearchScroll scrollRequest = new SearchScroll.Builder(scrollId, "1m").build();
            final JestResult scrollResult = JestUtils.execute(jestClient, scrollRequest, () -> "Couldn't process result of scroll query");
            final JsonArray scrollHits = Optional.of(scrollResult.getJsonObject())
                    .map(json -> asJsonObject(json.get("hits")))
                    .map(hits -> asJsonArray(hits.get("hits")))
                    .orElse(new JsonArray());

            // No more hits.
            if (scrollHits.size() == 0) {
                break;
            }

            final Bulk.Builder bulkRequestBuilder = new Bulk.Builder();
            for (JsonElement jsonElement : scrollHits) {
                final Map<String, Object> doc = Optional.ofNullable(asJsonObject(jsonElement))
                        .map(hitsJson -> asJsonObject(hitsJson.get("_source")))
                        .map(sourceJson -> gson.<Map<String, Object>>fromJson(sourceJson, type))
                        .orElse(Collections.emptyMap());
                final String id = (String) doc.remove("_id");

                bulkRequestBuilder.addAction(messages.prepareIndexRequest(target, doc, id));
            }

            bulkRequestBuilder.setParameter(Parameters.CONSISTENCY, "one");

            final BulkResult bulkResult = JestUtils.execute(jestClient, bulkRequestBuilder.build(), () -> "Couldn't bulk index messages into index " + target);

            final boolean hasFailedItems = !bulkResult.getFailedItems().isEmpty();
            LOG.info("Moving index <{}> to <{}>: Bulk indexed {} messages, took {} ms, failures: {}",
                    source,
                    target,
                    bulkResult.getItems().size(),
                    asLong(bulkResult.getJsonObject().get("took")),
                    hasFailedItems);

            if (hasFailedItems) {
                throw new ElasticsearchException("Failed to move a message. Check your indexer log.");
            }
        }
    }

    public void delete(String indexName) {
        JestUtils.execute(jestClient, new DeleteIndex.Builder(indexName).build(), () -> "Couldn't delete index " + indexName);
        eventBus.post(IndicesDeletedEvent.create(indexName));
    }

    public void close(String indexName) {
        JestUtils.execute(jestClient, new CloseIndex.Builder(indexName).build(), () -> "Couldn't close index " + indexName);
        eventBus.post(IndicesClosedEvent.create(indexName));
    }

    public long numberOfMessages(String indexName) throws IndexNotFoundException {
        return indexStats(indexName)
                .map(index -> asJsonObject(index.get("primaries")))
                .map(primaries -> asJsonObject(primaries.get("docs")))
                .map(docs -> asLong(docs.get("count")))
                .orElse(0L);
    }

    private Map<String, JsonElement> getAllWithShardLevel(final Collection<String> indices) {
        final Stats request = new Stats.Builder()
                .addIndex(indices)
                .setParameter("level", "shards")
                .build();
        final JestResult jestResult = JestUtils.execute(jestClient, request, () -> "Couldn't fetch index stats of indices " + indices);
        final JsonObject responseJson = jestResult.getJsonObject();
        final int failedShards = Optional.ofNullable(responseJson)
                .map(json -> asJsonObject(json.get("_shards")))
                .map(json -> asInteger(json.get("failed")))
                .orElse(0);

        if (failedShards > 0) {
            throw new ElasticsearchException("Index stats response contains failed shards, response is incomplete");
        }

        return Optional.ofNullable(responseJson)
                .map(json -> asJsonObject(json.get("indices")))
                .map(GsonUtils::entrySetAsMap)
                .orElse(Collections.emptyMap());
    }

    public Map<String, JsonElement> getIndexStats(final IndexSet indexSet) {
        return getIndexStats(Collections.singleton(indexSet.getIndexWildcard()));
    }

    public Map<String, JsonElement> getIndexStats(final Collection<String> indices) {
        final Stats request = new Stats.Builder()
                .addIndex(indices)
                .docs(true)
                .store(true)
                .build();

        final JestResult jestResult = JestUtils.execute(jestClient, request, () -> "Couldn't check stats of indices " + indices);

        return Optional.ofNullable(asJsonObject(jestResult.getJsonObject()))
                .map(json -> asJsonObject(json.get("indices")))
                .map(GsonUtils::entrySetAsMap)
                .orElse(Collections.emptyMap());
    }

    private Optional<JsonObject> indexStats(final String indexName) {
        final Stats request = new Stats.Builder()
                .addIndex(indexName)
                .build();

        final JestResult jestResult = JestUtils.execute(jestClient, request, () -> "Couldn't check stats of index " + indexName);

        return Optional.ofNullable(asJsonObject(jestResult.getJsonObject()))
                .map(json -> asJsonObject(json.get("indices")))
                .map(indices -> asJsonObject(indices.get(indexName)));
    }

    private Optional<JsonObject> indexStatsWithShardLevel(final String indexName) {
        final Stats request = new Stats.Builder()
                .addIndex(indexName)
                .setParameter("level", "shards")
                .ignoreUnavailable(true)
                .build();

        final JestResult jestResult = JestUtils.execute(jestClient, request, () -> "Couldn't check stats of index " + indexName);

        return Optional.ofNullable(asJsonObject(jestResult.getJsonObject()))
                .map(json -> asJsonObject(json.get("indices")))
                .map(indices -> asJsonObject(indices.get(indexName)));
    }

    public boolean exists(String indexName) {
        try {
            return jestClient.execute(new IndicesExists.Builder(indexName).build()).isSucceeded();
        } catch (IOException e) {
            throw new ElasticsearchException("Couldn't check existence of index " + indexName, e);
        }
    }

    public boolean aliasExists(String alias) {
        try {
            return jestClient.execute(new AliasExists.Builder().alias(alias).build()).isSucceeded();
        } catch (IOException e) {
            throw new ElasticsearchException("Couldn't check existence of alias " + alias, e);
        }
    }

    @NotNull
    public Map<String, Set<String>> getIndexNamesAndAliases(String indexPattern) {
        // only request indices matching the name or pattern in `indexPattern` and only get the alias names for each index,
        // not the settings or mappings
        final GetAliases request = new GetAliases.Builder().addIndex(indexPattern).build();

        final JestResult jestResult = JestUtils.execute(jestClient, request, () -> "Couldn't collect aliases for index pattern " + indexPattern);

        final ImmutableMap.Builder<String, Set<String>> indexAliasesBuilder = ImmutableMap.builder();
        for (Map.Entry<String, JsonElement> entry : jestResult.getJsonObject().entrySet()) {
            final JsonObject aliasMetaData = asJsonObject(entry.getValue());
            if (aliasMetaData != null) {
                final ImmutableSet.Builder<String> aliasesBuilder = ImmutableSet.builder();
                for (Map.Entry<String, JsonElement> aliasesEntry : aliasMetaData.entrySet()) {
                    aliasesBuilder.add(aliasesEntry.getKey());
                }
                indexAliasesBuilder.put(entry.getKey(), aliasesBuilder.build());
            }
        }

        return indexAliasesBuilder.build();
    }

    public Optional<String> aliasTarget(String alias) throws TooManyAliasesException {
        final GetAliases request = new GetAliases.Builder().build();
        final JestResult jestResult = JestUtils.execute(jestClient, request, () -> "Couldn't collect indices for alias " + alias);

        // The ES return value of this has an awkward format: The first key of the hash is the target index. Thanks.
        final ImmutableSet.Builder<String> indicesBuilder = ImmutableSet.builder();
        for (Map.Entry<String, JsonElement> entry : jestResult.getJsonObject().entrySet()) {
            final String indexName = entry.getKey();
            Optional.of(entry.getValue())
                    .map(GsonUtils::asJsonObject)
                    .map(json -> asJsonObject(json.get("aliases")))
                    .map(JsonObject::entrySet)
                    .filter(aliases -> !aliases.isEmpty())
                    .filter(aliases -> aliases.stream().anyMatch(aliasEntry -> aliasEntry.getKey().equals(alias)))
                    .ifPresent(x -> indicesBuilder.add(indexName));
        }

        final Set<String> indices = indicesBuilder.build();
        if (indices.size() > 1) {
            throw new TooManyAliasesException(indices);
        }

        return indices.stream().findFirst();
    }

    private void ensureIndexTemplate(IndexSet indexSet) {
        final IndexSetConfig indexSetConfig = indexSet.getConfig();
        final String templateName = indexSetConfig.indexTemplateName();
        final Map<String, Object> template = indexMapping.messageTemplate(indexSet.getIndexWildcard(), indexSetConfig.indexAnalyzer(), -1);

        final PutTemplate request = new PutTemplate.Builder(templateName, template).build();

        final JestResult jestResult = JestUtils.execute(jestClient, request, () -> "Unable to create index template " + templateName);

        if (jestResult.isSucceeded()) {
            LOG.info("Successfully created index template {}", templateName);
        }
    }

    public void deleteIndexTemplate(IndexSet indexSet) {
        final String templateName = indexSet.getConfig().indexTemplateName();
        final JestResult result = JestUtils.execute(jestClient, new DeleteTemplate.Builder(templateName).build(), () -> "Unable to delete the Graylog index template " + templateName);
        if (result.isSucceeded()) {
            LOG.info("Successfully deleted index template {}", templateName);
        }
    }

    public boolean create(String indexName, IndexSet indexSet) {
        return create(indexName, indexSet, Collections.emptyMap());
    }

    public boolean create(String indexName, IndexSet indexSet, Map<String, Object> customSettings) {
        final Map<String, Object> settings = new HashMap<>();
        settings.put("number_of_shards", indexSet.getConfig().shards());
        settings.put("number_of_replicas", indexSet.getConfig().replicas());
        settings.putAll(customSettings);

        final CreateIndex request = new CreateIndex.Builder(indexName)
                .settings(settings)
                .build();

        // Make sure our index template exists before creating an index!
        ensureIndexTemplate(indexSet);

        final JestResult jestResult;
        try {
            jestResult = jestClient.execute(request);
        } catch (IOException e) {
            throw new ElasticsearchException("Couldn't create index " + indexName, e);
        }

        final boolean succeeded = jestResult.isSucceeded();
        if (succeeded) {
            auditEventSender.success(AuditActor.system(nodeId), ES_INDEX_CREATE, ImmutableMap.of("indexName", indexName));
        } else {
            LOG.warn("Couldn't create index {}", indexName);
            auditEventSender.failure(AuditActor.system(nodeId), ES_INDEX_CREATE, ImmutableMap.of("indexName", indexName));
        }
        return succeeded;
    }

    public Map<String, Set<String>> getAllMessageFieldsForIndices(final String[] writeIndexWildcards) {
        final String indices = String.join(",", writeIndexWildcards);
        final State request = new State.Builder()
                .indices(indices)
                .withMetadata()
                .build();

        final JestResult jestResult = JestUtils.execute(jestClient, request, () -> "Couldn't read cluster state for indices " + indices);

        final JsonObject indicesJson = getClusterStateIndicesMetadata(jestResult.getJsonObject());
        final ImmutableMap.Builder<String, Set<String>> fields = ImmutableMap.builder();
        for (Map.Entry<String, JsonElement> entry : indicesJson.entrySet()) {
            final String indexName = entry.getKey();
            final Set<String> fieldNames = Optional.ofNullable(asJsonObject(entry.getValue()))
                    .map(index -> asJsonObject(index.get("mappings")))
                    .map(mappings -> asJsonObject(mappings.get(IndexMapping.TYPE_MESSAGE)))
                    .map(messageType -> asJsonObject(messageType.get("properties")))
                    .map(JsonObject::entrySet)
                    .map(Set::stream)
                    .orElseGet(Stream::empty)
                    .map(Map.Entry::getKey)
                    .collect(toSet());

            if (!fieldNames.isEmpty()) {
                fields.put(indexName, fieldNames);
            }
        }

        return fields.build();
    }

    public Set<String> getAllMessageFields(final String[] writeIndexWildcards) {
        return getAllMessageFieldsForIndices(writeIndexWildcards)
                .values()
                .stream()
                .reduce((strings, strings2) -> ImmutableSet.<String>builder().addAll(strings).addAll(strings2).build())
                .orElse(Collections.emptySet());
    }

    public void setReadOnly(String index) {
        // https://www.elastic.co/guide/en/elasticsearch/reference/2.4/indices-update-settings.html
        final Map<String, Object> settings = ImmutableMap.of(
                "index", ImmutableMap.of("blocks",
                        ImmutableMap.of(
                                "write", true, // Block writing.
                                "read", false, // Allow reading.
                                "metadata", false) // Allow getting metadata.
                )
        );

        final UpdateSettings request = new UpdateSettings.Builder(settings).addIndex(index).build();
        JestUtils.execute(jestClient, request, () -> "Couldn't set index " + index + " to read-only");
    }

    public void flush(String index) {
        JestUtils.execute(jestClient, new Flush.Builder().addIndex(index).force().build(), () -> "Couldn't flush index " + index);
    }

    public Map<String, Object> reopenIndexSettings() {
        return ImmutableMap.of("index", ImmutableMap.of(REOPENED_INDEX_SETTING, true));
    }

    public void reopenIndex(String index) {
        // Mark this index as re-opened. It will never be touched by retention.
        final Map<String, Object> settings = reopenIndexSettings();
        final UpdateSettings request = new UpdateSettings.Builder(settings).addIndex(index).build();

        JestUtils.execute(jestClient, request, () -> "Couldn't update settings of index " + index);

        // Open index.
        openIndex(index);
    }

    private void openIndex(String index) {
        JestUtils.execute(jestClient, new OpenIndex.Builder(index).build(), () -> "Couldn't open index " + index);
        eventBus.post(IndicesReopenedEvent.create(index));
    }

    public boolean isReopened(String indexName) {
        final JestResult jestResult = JestUtils.execute(jestClient, new State.Builder().withMetadata().build(), () -> "Couldn't read cluster state for index " + indexName);

        final JsonObject indexJson = Optional.ofNullable(asJsonObject(jestResult.getJsonObject()))
                .map(response -> asJsonObject(response.get("metadata")))
                .map(metadata -> asJsonObject(metadata.get("indices")))
                .map(indices -> getIndexSettings(indices, indexName))
                .orElse(new JsonObject());

        return checkForReopened(indexJson);
    }

    public Map<String, Boolean> areReopened(Collection<String> indices) {
        final JestResult jestResult = JestUtils.execute(jestClient, new State.Builder().withMetadata().build(), () -> "Couldn't read cluster state for indices " + indices);

        final JsonObject indicesJson = getClusterStateIndicesMetadata(jestResult.getJsonObject());
        return indices.stream().collect(
                Collectors.toMap(Function.identity(), index -> checkForReopened(getIndexSettings(indicesJson, index)))
        );
    }

    private JsonObject getIndexSettings(JsonObject indicesJson, String index) {
        return Optional.ofNullable(asJsonObject(indicesJson))
                .map(indices -> asJsonObject(indices.get(index)))
                .map(idx -> asJsonObject(idx.get("settings")))
                .map(settings -> asJsonObject(settings.get("index")))
                .orElse(new JsonObject());
    }

    private boolean checkForReopened(@Nullable JsonObject indexSettings) {
        return Optional.ofNullable(indexSettings)
                .map(settings -> asString(settings.get(REOPENED_INDEX_SETTING))) // WTF, why is this a string?
                .map(Boolean::parseBoolean)
                .orElse(false);
    }

    public Set<String> getClosedIndices(final Collection<String> indices) {
        final JsonArray catIndices = catIndices(indices, "index", "status");

        final ImmutableSet.Builder<String> closedIndices = ImmutableSet.builder();
        for (JsonElement jsonElement : catIndices) {
            if (jsonElement.isJsonObject()) {
                final JsonObject jsonObject = jsonElement.getAsJsonObject();
                final String index = GsonUtils.asString(jsonObject.get("index"));
                final String status = GsonUtils.asString(jsonObject.get("status"));
                if(index != null && "close".equals(status)){
                    closedIndices.add(index);
                }
            }
        }

        return closedIndices.build();
    }

    public Set<String> getClosedIndices(final IndexSet indexSet) {
        return getClosedIndices(Collections.singleton(indexSet.getIndexWildcard()));
    }

    /**
     * Retrieve the response for the <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-indices.html">cat indices</a> request from Elasticsearch.
     *
     * @param fields The fields to show, see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-indices.html">cat indices API</a>.
     * @return A {@link JsonArray} with the result of the cat indices request.
     */
    private JsonArray catIndices(Collection<String> indices, String... fields) {
        final String fieldNames = String.join(",", fields);
        final Cat request = new Cat.IndicesBuilder()
                .addIndex(indices)
                .setParameter("h", fieldNames)
                .build();
        final CatResult response = JestUtils.execute(jestClient, request, () -> "Unable to read information for indices " + indices);
        return Optional.of(response.getJsonObject())
                .map(json -> GsonUtils.asJsonArray(json.get("result")))
                .orElse(new JsonArray());
    }

    private JsonObject getClusterStateIndicesMetadata(JsonObject clusterStateJson) {
        return Optional.ofNullable(clusterStateJson)
                .map(json -> asJsonObject(json.get("metadata")))
                .map(metadata -> asJsonObject(metadata.get("indices")))
                .orElse(new JsonObject());
    }

    public Set<String> getReopenedIndices(final Collection<String> indices) {
        final String indexList = String.join(",", indices);
        final State request = new State.Builder().withMetadata().indices(indexList).build();

        final JestResult jestResult = JestUtils.execute(jestClient, request, () -> "Couldn't read cluster state for reopened indices " + indices);
        final JsonObject indicesJson = getClusterStateIndicesMetadata(jestResult.getJsonObject());
        final ImmutableSet.Builder<String> reopenedIndices = ImmutableSet.builder();

        for (Map.Entry<String, JsonElement> entry : indicesJson.entrySet()) {
            final String indexName = entry.getKey();
            final JsonElement value = entry.getValue();
            if (value.isJsonObject()) {
                final JsonObject indexSettingsJson = value.getAsJsonObject();
                final JsonObject indexSettings = getIndexSettings(indexSettingsJson, indexName);
                if (checkForReopened(indexSettings)) {
                    reopenedIndices.add(indexName);
                }
            }
        }

        return reopenedIndices.build();
    }

    public Set<String> getReopenedIndices(final IndexSet indexSet) {
        return getReopenedIndices(Collections.singleton(indexSet.getIndexWildcard()));
    }

    public Optional<IndexStatistics> getIndexStats(String index) {
        return indexStatsWithShardLevel(index)
                .map(indexStats -> buildIndexStatistics(index, indexStats));
    }

    private IndexStatistics buildIndexStatistics(String index, JsonObject indexStats) {
        return IndexStatistics.create(index, indexStats);
    }

    public Optional<Long> getStoreSizeInBytes(String index) {
        final Stats request = new Stats.Builder()
                .addIndex(index)
                .store(true)
                .build();

        final JestResult jestResult = JestUtils.execute(jestClient, request, () -> "Couldn't check store stats of index " + index);

        return Optional.ofNullable(jestResult.getJsonObject())
                .map(json -> asJsonObject(json.get("indices")))
                .map(json -> asJsonObject(json.get(index)))
                .map(json -> asJsonObject(json.get("primaries")))
                .map(json -> asJsonObject(json.get("store")))
                .map(json -> asLong(json.get("size_in_bytes")));
    }

    public Set<IndexStatistics> getIndicesStats(final IndexSet indexSet) {
        return getIndicesStats(Collections.singleton(indexSet.getIndexWildcard()));
    }

    public Set<IndexStatistics> getIndicesStats(final Collection<String> indices) {
        final ImmutableSet.Builder<IndexStatistics> result = ImmutableSet.builder();
        for (Map.Entry<String, JsonElement> entry : getAllWithShardLevel(indices).entrySet()) {
            final String index = entry.getKey();
            Optional.ofNullable(asJsonObject(entry.getValue()))
                    .map(indexStats -> buildIndexStatistics(index, indexStats))
                    .ifPresent(result::add);
        }

        return result.build();
    }

    public void cycleAlias(String aliasName, String targetIndex) {
        final AddAliasMapping addAliasMapping = new AddAliasMapping.Builder(targetIndex, aliasName).build();
        JestUtils.execute(jestClient, new ModifyAliases.Builder(addAliasMapping).build(), () -> "Couldn't point alias " + aliasName + " to index " + targetIndex);
    }

    public void cycleAlias(String aliasName, String targetIndex, String oldIndex) {
        final AliasMapping addAliasMapping = new AddAliasMapping.Builder(targetIndex, aliasName).build();
        final AliasMapping removeAliasMapping = new RemoveAliasMapping.Builder(oldIndex, aliasName).build();
        final ModifyAliases request = new ModifyAliases.Builder(Arrays.asList(removeAliasMapping, addAliasMapping)).build();

        JestUtils.execute(jestClient, request, () -> "Couldn't switch alias " + aliasName + " from index " + oldIndex + " to index " + targetIndex);
    }

    public void removeAliases(String alias, Set<String> indices) {
        final AliasMapping removeAliasMapping = new RemoveAliasMapping.Builder(ImmutableList.copyOf(indices), alias).build();
        final ModifyAliases request = new ModifyAliases.Builder(removeAliasMapping).build();
        JestUtils.execute(jestClient, request, () -> "Couldn't remove alias " + alias + " from indices " + indices);
    }

    public void optimizeIndex(String index, int maxNumSegments, Duration timeout) {
        // TODO: Individual timeout?
        final ForceMerge request = new ForceMerge.Builder()
                .addIndex(index)
                .maxNumSegments(maxNumSegments)
                .flush(true)
                .onlyExpungeDeletes(false)
                .build();

        JestUtils.execute(jestClient, request, () -> "Couldn't force merge index " + index);
    }

    public Health.Status waitForRecovery(String index) {
        return waitForStatus(index, Health.Status.YELLOW);
    }

    private Health.Status waitForStatus(String index, Health.Status clusterHealthStatus) {
        LOG.debug("Waiting until index health status of index {} is {}", index, clusterHealthStatus);
        final Health request = new Health.Builder()
                .addIndex(index)
                .waitForStatus(clusterHealthStatus)
                .build();
        final JestResult jestResult = JestUtils.execute(jestClient, request, () -> "Couldn't read health status for index " + index);

        final String status = jestResult.getJsonObject().get("status").getAsString();
        return Health.Status.valueOf(status.toUpperCase(Locale.ENGLISH));
    }

    public Optional<DateTime> indexCreationDate(String index) {
        final GetSettings request = new GetSettings.Builder()
                .addIndex(index)
                .ignoreUnavailable(true)
                .build();
        final JestResult jestResult = JestUtils.execute(jestClient, request, () -> "Couldn't read settings of index " + index);

        return Optional.of(jestResult.getJsonObject())
                .map(json -> asJsonObject(json.get(index)))
                .map(json -> asJsonObject(json.get("settings")))
                .map(json -> asJsonObject(json.get("index")))
                .map(json -> asString(json.get("creation_date")))  // WTF, why is this a string?
                .map(Long::parseLong)
                .map(creationDate -> new DateTime(creationDate, DateTimeZone.UTC));
    }

    /**
     * Calculate min and max message timestamps in the given index.
     *
     * @param index Name of the index to query.
     * @return the timestamp stats in the given index, or {@code null} if they couldn't be calculated.
     * @see org.elasticsearch.search.aggregations.metrics.stats.Stats
     */
    public IndexRangeStats indexRangeStatsOfIndex(String index) {
        final FilterAggregationBuilder builder = AggregationBuilders.filter("agg")
                .filter(QueryBuilders.existsQuery(Message.FIELD_TIMESTAMP))
                .subAggregation(AggregationBuilders.min("ts_min").field(Message.FIELD_TIMESTAMP))
                .subAggregation(AggregationBuilders.max("ts_max").field(Message.FIELD_TIMESTAMP))
                .subAggregation(AggregationBuilders.terms("streams").field(Message.FIELD_STREAMS));
        final String query = searchSource()
                .aggregation(builder)
                .size(0)
                .toString();

        final Search request = new Search.Builder(query)
                .addIndex(index)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .ignoreUnavailable(true)
                .build();

        if (LOG.isDebugEnabled()) {
            final Gson gson = new GsonBuilder().setPrettyPrinting().create();
            LOG.debug("Index range query: _search/{}: {}",
                    index,
                    request.getData(gson));
        }

        final SearchResult result = JestUtils.execute(jestClient, request, () -> "Couldn't build index range of index " + index);

        final FilterAggregation f = result.getAggregations().getFilterAggregation("agg");
        if(f == null) {
            throw new IndexNotFoundException("Couldn't build index range of index " + index + " because it doesn't exist.");
        } else if (f.getCount() == 0L) {
            LOG.debug("No documents with attribute \"timestamp\" found in index <{}>", index);
            return IndexRangeStats.EMPTY;
        }

        final MinAggregation minAgg = f.getMinAggregation("ts_min");
        final DateTime min = new DateTime(minAgg.getMin().longValue(), DateTimeZone.UTC);
        final MaxAggregation maxAgg = f.getMaxAggregation("ts_max");
        final DateTime max = new DateTime(maxAgg.getMax().longValue(), DateTimeZone.UTC);
        // make sure we return an empty list, so we can differentiate between old indices that don't have this information
        // and newer ones that simply have no streams.
        final TermsAggregation streams = f.getTermsAggregation("streams");
        final List<String> streamIds = streams.getBuckets().stream()
                .map(TermsAggregation.Entry::getKeyAsString)
                .collect(toList());


        return IndexRangeStats.create(min, max, streamIds);
    }
}
