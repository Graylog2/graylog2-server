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
package org.graylog.storage.elasticsearch6;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.joschi.jadconfig.util.Duration;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
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
import org.graylog.shaded.elasticsearch5.org.elasticsearch.index.query.QueryBuilders;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.search.sort.FieldSortBuilder;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.search.sort.SortBuilders;
import org.graylog.storage.elasticsearch6.jest.JestUtils;
import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.indexer.IndexMapping;
import org.graylog2.indexer.IndexNotFoundException;
import org.graylog2.indexer.indices.HealthStatus;
import org.graylog2.indexer.indices.IndexMoveResult;
import org.graylog2.indexer.indices.IndexSettings;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.indices.IndicesAdapter;
import org.graylog2.indexer.indices.stats.IndexStatistics;
import org.graylog2.indexer.searches.IndexRangeStats;
import org.graylog2.jackson.TypeReferences;
import org.graylog2.plugin.Message;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toSet;
import static org.graylog.shaded.elasticsearch5.org.elasticsearch.search.builder.SearchSourceBuilder.searchSource;

public class IndicesAdapterES6 implements IndicesAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(IndicesAdapterES6.class);
    private final JestClient jestClient;
    private final ObjectMapper objectMapper;
    private final IndexingHelper indexingHelper;

    @Inject
    public IndicesAdapterES6(JestClient jestClient, ObjectMapper objectMapper, IndexingHelper indexingHelper) {
        this.jestClient = jestClient;
        this.objectMapper = objectMapper;
        this.indexingHelper = indexingHelper;
    }

    @Override
    public void move(String source, String target, Consumer<IndexMoveResult> resultCallback) {
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
                Optional.ofNullable(jsonElement.path("_source"))
                        .map(sourceJson -> objectMapper.<Map<String, Object>>convertValue(sourceJson, TypeReferences.MAP_STRING_OBJECT))
                        .ifPresent(doc -> {
                            final String id = (String) doc.remove("_id");
                            if (!Strings.isNullOrEmpty(id)) {
                                bulkRequestBuilder.addAction(indexingHelper.prepareIndexRequest(target, doc, id));
                            }
                        });
            }

            final BulkResult bulkResult = JestUtils.execute(jestClient, bulkRequestBuilder.build(), () -> "Couldn't bulk index messages into index " + target);

            final boolean hasFailedItems = !bulkResult.getFailedItems().isEmpty();
            final IndexMoveResult result = IndexMoveResult.create(bulkResult.getItems().size(), bulkResult.getJsonObject().path("took").asLong(), hasFailedItems);
            resultCallback.accept(result);
        }
    }

    @Override
    public void delete(String indexName) {
        JestUtils.execute(jestClient, new DeleteIndex.Builder(indexName).build(), () -> "Couldn't delete index " + indexName);
    }

    @Override
    public Set<String> resolveAlias(String alias) {
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

        return indicesBuilder.build();
    }

    @Override
    public void create(String indexName, IndexSettings indexSettings, String templateName, Map<String, Object> template) {
        final Map<String, Object> settings = new HashMap<>();
        settings.put("number_of_shards", indexSettings.shards());
        settings.put("number_of_replicas", indexSettings.replicas());

        final CreateIndex request = new CreateIndex.Builder(indexName)
                .settings(settings)
                .build();

        // Make sure our index template exists before creating an index!
        ensureIndexTemplate(templateName, template);

        final JestResult jestResult;
        try {
            jestResult = jestClient.execute(request);
        } catch (IOException e) {
            throw new ElasticsearchException("Couldn't create index " + indexName, e);
        }

        if (!jestResult.isSucceeded()){
            throw new ElasticsearchException(jestResult.getErrorMessage());
        }
    }

    @Override
    public boolean ensureIndexTemplate(String templateName, Map<String, Object> template) {
        final PutTemplate request = new PutTemplate.Builder(templateName, template).build();

        final JestResult jestResult = JestUtils.execute(jestClient, request, () -> "Unable to create index template " + templateName);
        return jestResult.isSucceeded();
    }

    @Override
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

    @Override
    public void openIndex(String index) {
        JestUtils.execute(jestClient, new OpenIndex.Builder(index).build(), () -> "Couldn't open index " + index);
    }

    @Override
    public void setReadOnly(String index) {
        // https://www.elastic.co/guide/en/elasticsearch/reference/6.8/indices-update-settings.html
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

    @Override
    public void flush(String index) {
        JestUtils.execute(jestClient, new Flush.Builder().addIndex(index).force().build(), () -> "Couldn't flush index " + index);
    }

    @Override
    public void markIndexReopened(String index) {
        final String aliasName = index + Indices.REOPENED_ALIAS_SUFFIX;
        final ModifyAliases request = new ModifyAliases.Builder(new AddAliasMapping.Builder(index, aliasName).build()).build();

        JestUtils.execute(jestClient, request, () -> "Couldn't create reopened alias for index " + index);
    }

    @Override
    public void removeAlias(String indexName, String alias) {
        JestUtils.execute(jestClient,
                new ModifyAliases.Builder(new RemoveAliasMapping.Builder(indexName, alias).build()).build(),
                () -> "Couldn't remove reopened alias for index " + indexName + " before closing.");
    }

    @Override
    public void removeAliases(Set<String> indices, String alias) {
        final AliasMapping removeAliasMapping = new RemoveAliasMapping.Builder(ImmutableList.copyOf(indices), alias).build();
        final ModifyAliases request = new ModifyAliases.Builder(removeAliasMapping).build();
        JestUtils.execute(jestClient, request, () -> "Couldn't remove alias " + alias + " from indices " + indices);
    }

    @Override
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

    @Override
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
        final Set<String> streamIds = streams.getBuckets().stream()
                .map(TermsAggregation.Entry::getKeyAsString)
                .collect(toSet());


        return IndexRangeStats.create(min, max, streamIds);
    }

    @Override
    public HealthStatus waitForRecovery(String index) {
        final Health.Status status = waitForStatus(index, Health.Status.YELLOW);
        return mapHealthStatus(status);
    }

    private HealthStatus mapHealthStatus(Health.Status status) {
        return HealthStatus.fromString(status.toString());
    }

    private Health.Status waitForStatus(String index, @SuppressWarnings("SameParameterValue") Health.Status clusterHealthStatus) {
        final Health request = new Health.Builder()
                .addIndex(index)
                .waitForStatus(clusterHealthStatus)
                .build();
        final JestResult jestResult = JestUtils.execute(jestClient, request, () -> "Couldn't read health status for index " + index);

        final String status = jestResult.getJsonObject().path("status").asText();
        return Health.Status.valueOf(status.toUpperCase(Locale.ENGLISH));
    }

    @Override
    public void close(String indexName) {
        JestUtils.execute(jestClient, new CloseIndex.Builder(indexName).build(), () -> "Couldn't close index " + indexName);
    }

    @Override
    public long numberOfMessages(String indexName) {
        return indexStats(indexName).path("primaries").path("docs").path("count").asLong();
    }

    private JsonNode indexStats(final String indexName) {
        final Stats request = new Stats.Builder()
                .addIndex(indexName)
                .build();

        final JestResult jestResult = JestUtils.execute(jestClient, request, () -> "Couldn't check stats of index " + indexName);

        return jestResult.getJsonObject().path("indices").path(indexName);
    }

    @Override
    public boolean aliasExists(String alias) throws IOException {
        final JestResult result = jestClient.execute(new GetSettings.Builder().addIndex(alias).build());
        return result.isSucceeded() && !Iterators.contains(result.getJsonObject().fieldNames(), alias);
    }

    @Override
    public Map<String, Set<String>> aliases(String indexPattern) {
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

    @Override
    public boolean deleteIndexTemplate(String templateName) {
        final JestResult result = JestUtils.execute(jestClient, new DeleteTemplate.Builder(templateName).build(), () -> "Unable to delete the Graylog index template " + templateName);
        return result.isSucceeded();
    }

    @Override
    public Map<String, Set<String>> fieldsInIndices(String[] writeIndexWildcards) {
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

    @Override
    public Set<String> closedIndices(Collection<String> indices) {
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

    @Override
    public Set<IndexStatistics> indicesStats(Collection<String> indices) {
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

    @Override
    public Optional<IndexStatistics> getIndexStats(String index) {
        final JsonNode indexStats = indexStatsWithShardLevel(index);
        return indexStats.isMissingNode() ? Optional.empty() : Optional.of(buildIndexStatistics(index, indexStats));
    }

    @Override
    public JsonNode getIndexStats(final Collection<String> indices) {
        final Stats request = new Stats.Builder()
                .addIndex(indices)
                .docs(true)
                .store(true)
                .build();

        final JestResult jestResult = JestUtils.execute(jestClient, request, () -> "Couldn't check stats of indices " + indices);

        return jestResult.getJsonObject().path("indices");
    }

    @Override
    public boolean exists(String indexName) throws IOException {
        final JestResult result = jestClient.execute(new GetSettings.Builder().addIndex(indexName).build());
        return result.isSucceeded() && Iterators.contains(result.getJsonObject().fieldNames(), indexName);
    }

    @Override
    public Set<String> indices(String indexWildcard, List<String> status, String indexSetId) {
        final Cat catRequest = new Cat.IndicesBuilder()
                .addIndex(indexWildcard)
                .setParameter("h", "index,status")
                .build();

        final CatResult result = JestUtils.execute(jestClient, catRequest,
                () -> "Couldn't get index list for index set <" + indexSetId + ">");

        return StreamSupport.stream(result.getJsonObject().path("result").spliterator(), false)
                .filter(cat -> status.isEmpty() || status.contains(cat.path("status").asText()))
                .map(cat -> cat.path("index").asText())
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<Long> storeSizeInBytes(String index) {
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

    @Override
    public void cycleAlias(String aliasName, String targetIndex) {
        final AddAliasMapping addAliasMapping = new AddAliasMapping.Builder(targetIndex, aliasName).build();
        JestUtils.execute(jestClient, new ModifyAliases.Builder(addAliasMapping).build(), () -> "Couldn't point alias " + aliasName + " to index " + targetIndex);
    }

    @Override
    public void cycleAlias(String aliasName, String targetIndex, String oldIndex) {
        final AliasMapping addAliasMapping = new AddAliasMapping.Builder(targetIndex, aliasName).build();
        final AliasMapping removeAliasMapping = new RemoveAliasMapping.Builder(oldIndex, aliasName).build();
        final ModifyAliases request = new ModifyAliases.Builder(Arrays.asList(removeAliasMapping, addAliasMapping)).build();

        JestUtils.execute(jestClient, request, () -> "Couldn't switch alias " + aliasName + " from index " + oldIndex + " to index " + targetIndex);
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

    private IndexStatistics buildIndexStatistics(String index, JsonNode indexStats) {
        return IndexStatistics.create(index, indexStats);
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

    @Override
    public boolean isOpen(String index) {
        return getIndexState(index).equals("open");
    }

    @Override
    public boolean isClosed(String index) {
        return getIndexState(index).equals("close");
    }

    private String getIndexState(String index) {
        final State request = new State.Builder().indices(index).withMetadata().build();

        final JestResult response = JestUtils.execute(jestClient, request, () -> "Failed to get index metadata");

        return response.getJsonObject().path("metadata").path("indices").path(index).path("state").asText();
    }
}
