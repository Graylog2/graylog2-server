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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.joschi.jadconfig.util.Duration;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.eventbus.EventBus;
import com.google.common.primitives.Ints;
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
import io.searchbox.indices.OpenIndex;
import io.searchbox.indices.Stats;
import io.searchbox.indices.aliases.AddAliasMapping;
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
import org.apache.http.client.config.RequestConfig;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.graylog2.audit.AuditActor;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.indexer.IndexMapping;
import org.graylog2.indexer.IndexMappingFactory;
import org.graylog2.indexer.IndexMappingTemplate;
import org.graylog2.indexer.IndexNotFoundException;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.cluster.jest.JestUtils;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indices.events.IndicesClosedEvent;
import org.graylog2.indexer.indices.events.IndicesDeletedEvent;
import org.graylog2.indexer.indices.events.IndicesReopenedEvent;
import org.graylog2.indexer.indices.stats.IndexStatistics;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.indexer.searches.IndexRangeStats;
import org.graylog2.jackson.TypeReferences;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.system.NodeId;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;
import static org.elasticsearch.search.builder.SearchSourceBuilder.searchSource;
import static org.graylog2.audit.AuditEventTypes.ES_INDEX_CREATE;

@Singleton
public class Indices {
    private static final Logger LOG = LoggerFactory.getLogger(Indices.class);
    private static final String REOPENED_ALIAS_SUFFIX = "_reopened";

    private final JestClient jestClient;
    private final ObjectMapper objectMapper;
    private final IndexMappingFactory indexMappingFactory;
    private final Messages messages;
    private final NodeId nodeId;
    private final AuditEventSender auditEventSender;
    private final EventBus eventBus;

    @Inject
    public Indices(JestClient jestClient, ObjectMapper objectMapper, IndexMappingFactory indexMappingFactory,
                   Messages messages, NodeId nodeId, AuditEventSender auditEventSender,
                   EventBus eventBus) {
        this.jestClient = jestClient;
        this.objectMapper = objectMapper;
        this.indexMappingFactory = indexMappingFactory;
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
                .sort(SortBuilders.fieldSort(FieldSortBuilder.DOC_FIELD_NAME))
                .toString();

        final Search request = new Search.Builder(query)
                .setParameter(Parameters.SCROLL, "10s")
                .addIndex(source)
                .build();

        final SearchResult searchResult = JestUtils.execute(jestClient, request, () -> "Couldn't process search query response");

        final String scrollId = searchResult.getJsonObject().path("_scroll_id").asText(null);
        if (scrollId == null) {
            throw new ElasticsearchException("Couldn't find scroll ID in search query response");
        }

        while (true) {
            final SearchScroll scrollRequest = new SearchScroll.Builder(scrollId, "1m").build();
            final JestResult scrollResult = JestUtils.execute(jestClient, scrollRequest, () -> "Couldn't process result of scroll query");
            final JsonNode scrollHits = scrollResult.getJsonObject().path("hits").path("hits");

            // No more hits.
            if (scrollHits.size() == 0) {
                break;
            }

            final Bulk.Builder bulkRequestBuilder = new Bulk.Builder();
            for (JsonNode jsonElement : scrollHits) {
                final Map<String, Object> doc = Optional.ofNullable(jsonElement.path("_source"))
                        .map(sourceJson -> objectMapper.<Map<String, Object>>convertValue(sourceJson, TypeReferences.MAP_STRING_OBJECT))
                        .orElse(Collections.emptyMap());
                final String id = (String) doc.remove("_id");

                bulkRequestBuilder.addAction(messages.prepareIndexRequest(target, doc, id));
            }

            final BulkResult bulkResult = JestUtils.execute(jestClient, bulkRequestBuilder.build(), () -> "Couldn't bulk index messages into index " + target);

            final boolean hasFailedItems = !bulkResult.getFailedItems().isEmpty();
            LOG.info("Moving index <{}> to <{}>: Bulk indexed {} messages, took {} ms, failures: {}",
                    source,
                    target,
                    bulkResult.getItems().size(),
                    bulkResult.getJsonObject().path("took").asLong(),
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
        if (isReopened(indexName)) {
            JestUtils.execute(jestClient,
                new ModifyAliases.Builder(new RemoveAliasMapping.Builder(indexName, indexName + REOPENED_ALIAS_SUFFIX).build()).build(),
                () -> "Couldn't remove reopened alias for index " + indexName + " before closing.");
        }
        JestUtils.execute(jestClient, new CloseIndex.Builder(indexName).build(), () -> "Couldn't close index " + indexName);
        eventBus.post(IndicesClosedEvent.create(indexName));
    }

    public long numberOfMessages(String indexName) throws IndexNotFoundException {
        return indexStats(indexName).path("primaries").path("docs").path("count").asLong();
    }

    private JsonNode getAllWithShardLevel(final Collection<String> indices) {
        final Stats request = new Stats.Builder()
                .addIndex(indices)
                .setParameter("level", "shards")
                .build();
        final JestResult jestResult = JestUtils.execute(jestClient, request, () -> "Couldn't fetch index stats of indices " + indices);
        final JsonNode responseJson = jestResult.getJsonObject();
        final int failedShards = responseJson.path("_shards").path("failed").asInt();

        if (failedShards > 0) {
            throw new ElasticsearchException("Index stats response contains failed shards, response is incomplete");
        }

        return responseJson.path("indices");
    }

    public JsonNode getIndexStats(final IndexSet indexSet) {
        return getIndexStats(Collections.singleton(indexSet.getIndexWildcard()));
    }

    private JsonNode getIndexStats(final Collection<String> indices) {
        final Stats request = new Stats.Builder()
                .addIndex(indices)
                .docs(true)
                .store(true)
                .build();

        final JestResult jestResult = JestUtils.execute(jestClient, request, () -> "Couldn't check stats of indices " + indices);

        return jestResult.getJsonObject().path("indices");
    }

    private JsonNode indexStats(final String indexName) {
        final Stats request = new Stats.Builder()
                .addIndex(indexName)
                .build();

        final JestResult jestResult = JestUtils.execute(jestClient, request, () -> "Couldn't check stats of index " + indexName);

        return jestResult.getJsonObject().path("indices").path(indexName);
    }

    private JsonNode indexStatsWithShardLevel(final String indexName) {
        final Stats request = new Stats.Builder()
                .addIndex(indexName)
                .setParameter("level", "shards")
                .ignoreUnavailable(true)
                .build();

        final JestResult jestResult = JestUtils.execute(jestClient, request, () -> "Couldn't check stats of index " + indexName);

        return jestResult.getJsonObject().path("indices").path(indexName);
    }

    /**
     * Check if a given name is an existing index.
     *
     * @param indexName Name of the index to check presence for.
     * @return {@code true} if indexName is an existing index, {@code false} if it is non-existing or an alias.
     */
    public boolean exists(String indexName) {
        try {
            final JestResult result = jestClient.execute(new GetSettings.Builder().addIndex(indexName).build());
            return result.isSucceeded() && Iterators.contains(result.getJsonObject().fieldNames(), indexName);
        } catch (IOException e) {
            throw new ElasticsearchException("Couldn't check existence of index " + indexName, e);
        }
    }

    /**
     * Check if a given name is an existing alias.
     *
     * @param alias Name of the alias to check presence for.
     * @return {@code true} if alias is an existing alias, {@code false} if it is non-existing or an index.
     */
    public boolean aliasExists(String alias) {
        try {
            final JestResult result = jestClient.execute(new GetSettings.Builder().addIndex(alias).build());
            return result.isSucceeded() && !Iterators.contains(result.getJsonObject().fieldNames(), alias);
        } catch (IOException e) {
            throw new ElasticsearchException("Couldn't check existence of alias " + alias, e);
        }
    }

    /**
     * Returns index names and their aliases. This only returns indices which actually have an alias.
     */
    @NotNull
    public Map<String, Set<String>> getIndexNamesAndAliases(String indexPattern) {
        // only request indices matching the name or pattern in `indexPattern` and only get the alias names for each index,
        // not the settings or mappings
        final GetAliases request = new GetAliases.Builder()
                .addIndex(indexPattern)
                // ES 6 changed the "expand_wildcards" default value for the /_alias API from "open" to "all".
                // Since our code expects only open indices to be returned, we have to explicitly set the parameter now.
                .setParameter("expand_wildcards", "open")
                .build();

        final JestResult jestResult = JestUtils.execute(jestClient, request, () -> "Couldn't collect aliases for index pattern " + indexPattern);

        final ImmutableMap.Builder<String, Set<String>> indexAliasesBuilder = ImmutableMap.builder();
        final Iterator<Map.Entry<String, JsonNode>> it = jestResult.getJsonObject().fields();
        while (it.hasNext()) {
            final Map.Entry<String, JsonNode> entry = it.next();
            final String indexName = entry.getKey();
            final JsonNode aliasMetaData = entry.getValue().path("aliases");
            if (aliasMetaData.isObject()) {
                final ImmutableSet<String> aliasNames = ImmutableSet.copyOf(aliasMetaData.fieldNames());
                indexAliasesBuilder.put(indexName, aliasNames);
            }
        }

        return indexAliasesBuilder.build();
    }

    public Optional<String> aliasTarget(String alias) throws TooManyAliasesException {
        // TODO: This is basically getting all indices and later we filter out the alias we want to check for.
        //       This can be done in a more efficient way by either using the /_cat/aliases/<alias-name> API or
        //       the regular /_alias/<alias-name> API.
        final GetAliases request = new GetAliases.Builder().build();
        final JestResult jestResult = JestUtils.execute(jestClient, request, () -> "Couldn't collect indices for alias " + alias);

        // The ES return value of this has an awkward format: The first key of the hash is the target index. Thanks.
        final ImmutableSet.Builder<String> indicesBuilder = ImmutableSet.builder();
        final Iterator<Map.Entry<String, JsonNode>> it = jestResult.getJsonObject().fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            final String indexName = entry.getKey();
            Optional.of(entry.getValue())
                    .map(json -> json.path("aliases"))
                    .map(JsonNode::fields)
                    .map(ImmutableList::copyOf)
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

    public void ensureIndexTemplate(IndexSet indexSet) {
        final IndexSetConfig indexSetConfig = indexSet.getConfig();
        final String templateName = indexSetConfig.indexTemplateName();
        final IndexMappingTemplate indexMapping = indexMappingFactory.createIndexMapping(indexSetConfig.indexTemplateType().orElse(IndexSetConfig.DEFAULT_INDEX_TEMPLATE_TYPE));
        final Map<String, Object> template = indexMapping.toTemplate(indexSetConfig, indexSet.getIndexWildcard(), -1);

        final PutTemplate request = new PutTemplate.Builder(templateName, template).build();

        final JestResult jestResult = JestUtils.execute(jestClient, request, () -> "Unable to create index template " + templateName);

        if (jestResult.isSucceeded()) {
            LOG.info("Successfully created index template {}", templateName);
        }
    }

    /**
     * Returns the generated Elasticsearch index template for the given index set.
     *
     * @param indexSet the index set
     * @return the generated index template
     */
    public Map<String, Object> getIndexTemplate(IndexSet indexSet) {
        final String indexWildcard = indexSet.getIndexWildcard();

        return indexMappingFactory.createIndexMapping(indexSet.getConfig().indexTemplateType().orElse(IndexSetConfig.DEFAULT_INDEX_TEMPLATE_TYPE)).toTemplate(indexSet.getConfig(), indexWildcard);
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
            LOG.warn("Couldn't create index {}. Error: {}", indexName, jestResult.getErrorMessage());
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

        final JsonNode indicesJson = getClusterStateIndicesMetadata(jestResult.getJsonObject());
        final ImmutableMap.Builder<String, Set<String>> fields = ImmutableMap.builder();
        final Iterator<Map.Entry<String, JsonNode>> it = indicesJson.fields();
        while (it.hasNext()) {
            final Map.Entry<String, JsonNode> entry = it.next();
            final String indexName = entry.getKey();
            final Set<String> fieldNames = ImmutableSet.copyOf(
                    entry.getValue()
                            .path("mappings")
                            .path(IndexMapping.TYPE_MESSAGE)
                            .path("properties").fieldNames()
            );
            if (!fieldNames.isEmpty()) {
                fields.put(indexName, fieldNames);
            }
        }

        return fields.build();
    }

    public Set<String> getAllMessageFields(final String[] writeIndexWildcards) {
        final Map<String, Set<String>> fieldsForIndices = getAllMessageFieldsForIndices(writeIndexWildcards);
        final ImmutableSet.Builder<String> result = ImmutableSet.builder();
        for (Set<String> fields : fieldsForIndices.values()) {
            result.addAll(fields);
        }
        return result.build();
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

    public void reopenIndex(String index) {
        // Mark this index as re-opened. It will never be touched by retention.
        markIndexReopened(index);

        // Open index.
        openIndex(index);
    }

    public String markIndexReopened(String index) {
        final String aliasName = index + REOPENED_ALIAS_SUFFIX;
        final ModifyAliases request = new ModifyAliases.Builder(new AddAliasMapping.Builder(index, aliasName).build()).build();

        JestUtils.execute(jestClient, request, () -> "Couldn't create reopened alias for index " + index);

        return aliasName;
    }

    private void openIndex(String index) {
        JestUtils.execute(jestClient, new OpenIndex.Builder(index).build(), () -> "Couldn't open index " + index);
        eventBus.post(IndicesReopenedEvent.create(index));
    }

    public boolean isReopened(String indexName) {
        final Optional<String> aliasTarget = aliasTarget(indexName + REOPENED_ALIAS_SUFFIX);

        return aliasTarget.map(target -> target.equals(indexName)).orElse(false);
    }

    public Map<String, Boolean> areReopened(Collection<String> indices) {
        return indices.stream().collect(Collectors.toMap(Function.identity(), this::isReopened));
    }

    public Set<String> getClosedIndices(final Collection<String> indices) {
        final JsonNode catIndices = catIndices(indices, "index", "status");

        final ImmutableSet.Builder<String> closedIndices = ImmutableSet.builder();
        for (JsonNode jsonElement : catIndices) {
            if (jsonElement.isObject()) {
                final String index = jsonElement.path("index").asText(null);
                final String status = jsonElement.path("status").asText(null);
                if (index != null && "close".equals(status)) {
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
     * Retrieves all indices in the given {@link IndexSet}.
     * <p>
     * If any status filter parameter are present, only indices with the given status are returned.
     *
     * @param indexSet the index set
     * @param statusFilter only indices with the given status are returned. (available: "open", "close")
     * @return the set of indices in the given index set
     */
    public Set<String> getIndices(final IndexSet indexSet, final String... statusFilter) {
        final List<String> status = Arrays.asList(statusFilter);
        final Cat catRequest = new Cat.IndicesBuilder()
                .addIndex(indexSet.getIndexWildcard())
                .setParameter("h", "index,status")
                .build();

        final CatResult result = JestUtils.execute(jestClient, catRequest,
                () -> "Couldn't get index list for index set <" + indexSet.getConfig().id() + ">");

        return StreamSupport.stream(result.getJsonObject().path("result").spliterator(), false)
                .filter(cat -> status.isEmpty() || status.contains(cat.path("status").asText()))
                .map(cat -> cat.path("index").asText())
                .collect(Collectors.toSet());
    }

    public boolean isClosed(final String indexName) {
        return getClosedIndices(Collections.singleton(indexName)).contains(indexName);
    }

    /**
     * Retrieve the response for the <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-indices.html">cat indices</a> request from Elasticsearch.
     *
     * @param fields The fields to show, see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-indices.html">cat indices API</a>.
     * @return A {@link JsonNode} with the result of the cat indices request.
     */
    private JsonNode catIndices(Collection<String> indices, String... fields) {
        final String fieldNames = String.join(",", fields);
        final Cat request = new Cat.IndicesBuilder()
                .addIndex(indices)
                .setParameter("h", fieldNames)
                .build();
        final CatResult response = JestUtils.execute(jestClient, request, () -> "Unable to read information for indices " + indices);
        return response.getJsonObject().path("result");
    }

    private JsonNode getClusterStateIndicesMetadata(JsonNode clusterStateJson) {
        return clusterStateJson.path("metadata").path("indices");
    }

    public Set<String> getReopenedIndices(final Collection<String> indices) {
        return indices.stream()
            .filter(this::isReopened)
            .collect(Collectors.toSet());
    }

    public Set<String> getReopenedIndices(final IndexSet indexSet) {
        return getReopenedIndices(Collections.singleton(indexSet.getIndexWildcard()));
    }

    public Optional<IndexStatistics> getIndexStats(String index) {
        final JsonNode indexStats = indexStatsWithShardLevel(index);
        return indexStats.isMissingNode() ? Optional.empty() : Optional.of(buildIndexStatistics(index, indexStats));
    }

    private IndexStatistics buildIndexStatistics(String index, JsonNode indexStats) {
        return IndexStatistics.create(index, indexStats);
    }

    public Optional<Long> getStoreSizeInBytes(String index) {
        final Stats request = new Stats.Builder()
                .addIndex(index)
                .store(true)
                .build();

        final JestResult jestResult = JestUtils.execute(jestClient, request, () -> "Couldn't check store stats of index " + index);
        final JsonNode sizeInBytes = jestResult.getJsonObject()
                .path("indices")
                .path(index)
                .path("primaries")
                .path("store")
                .path("size_in_bytes");
        return Optional.of(sizeInBytes).filter(JsonNode::isNumber).map(JsonNode::asLong);
    }

    public Set<IndexStatistics> getIndicesStats(final IndexSet indexSet) {
        return getIndicesStats(Collections.singleton(indexSet.getIndexWildcard()));
    }

    public Set<IndexStatistics> getIndicesStats(final Collection<String> indices) {
        final ImmutableSet.Builder<IndexStatistics> result = ImmutableSet.builder();

        final JsonNode allWithShardLevel = getAllWithShardLevel(indices);
        final Iterator<Map.Entry<String, JsonNode>> fields = allWithShardLevel.fields();
        while (fields.hasNext()) {
            final Map.Entry<String, JsonNode> entry = fields.next();
            final String index = entry.getKey();
            final JsonNode indexStats = entry.getValue();
            if (indexStats.isObject()) {
                result.add(buildIndexStatistics(index, indexStats));
            }
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
        final RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(Ints.saturatedCast(timeout.toMilliseconds()))
                .build();

        final ForceMerge request = new ForceMerge.Builder()
                .addIndex(index)
                .maxNumSegments(maxNumSegments)
                .flush(true)
                .onlyExpungeDeletes(false)
                .build();

        JestUtils.execute(jestClient, requestConfig, request, () -> "Couldn't force merge index " + index);
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

        final String status = jestResult.getJsonObject().path("status").asText();
        return Health.Status.valueOf(status.toUpperCase(Locale.ENGLISH));
    }

    public Optional<DateTime> indexCreationDate(String index) {
        final GetSettings request = new GetSettings.Builder()
                .addIndex(index)
                .ignoreUnavailable(true)
                .build();
        final JestResult jestResult = JestUtils.execute(jestClient, request, () -> "Couldn't read settings of index " + index);

        return Optional.of(jestResult.getJsonObject().path(index).path("settings").path("index").path("creation_date"))
                .filter(JsonNode::isValueNode)
                .map(JsonNode::asLong)
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
        final FilterAggregationBuilder builder = AggregationBuilders.filter("agg", QueryBuilders.existsQuery(Message.FIELD_TIMESTAMP))
                .subAggregation(AggregationBuilders.min("ts_min").field(Message.FIELD_TIMESTAMP))
                .subAggregation(AggregationBuilders.max("ts_max").field(Message.FIELD_TIMESTAMP))
                .subAggregation(AggregationBuilders.terms("streams").size(Integer.MAX_VALUE).field(Message.FIELD_STREAMS));
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
            String data = "{}";
            try {
                data = request.getData(objectMapper.copy().enable(SerializationFeature.INDENT_OUTPUT));
            } catch (IOException e) {
                LOG.debug("Couldn't pretty print request payload", e);
            }
            LOG.debug("Index range query: _search/{}: {}", index, data);
        }

        final SearchResult result = JestUtils.execute(jestClient, request, () -> "Couldn't build index range of index " + index);

        final FilterAggregation f = result.getAggregations().getFilterAggregation("agg");
        if (f == null) {
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
