package org.graylog2.migrations;

import com.github.zafarkhaja.semver.Version;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.cluster.State;
import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.MongoIndexSet;
import org.graylog2.indexer.cluster.Node;
import org.graylog2.indexer.cluster.jest.JestUtils;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indices.Indices;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.graylog2.indexer.gson.GsonUtils.asJsonObject;
import static org.graylog2.indexer.gson.GsonUtils.asString;

public class V20170607164210_MigrateReopenedIndicesToAliases extends Migration {
    private static final String REOPENED_INDEX_SETTING = "graylog2_reopened";

    private final Version elasticsearchVersion;
    private final IndexSetService indexSetService;
    private final MongoIndexSet.Factory mongoIndexSetFactory;
    private final Indices indices;
    private final JestClient jestClient;

    @Inject
    public V20170607164210_MigrateReopenedIndicesToAliases(Node node,
                                                           IndexSetService indexSetService,
                                                           MongoIndexSet.Factory mongoIndexSetFactory,
                                                           Indices indices,
                                                           JestClient jestClient) {
        this.elasticsearchVersion = node.getVersion().orElseThrow(() -> new ElasticsearchException("Unable to retrieve Elasticsearch version."));
        this.indexSetService = indexSetService;
        this.mongoIndexSetFactory = mongoIndexSetFactory;
        this.indices = indices;
        this.jestClient = jestClient;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2017-06-07T16:42:10Z");
    }

    // Create aliases for legacy reopened indices.
    @Override
    public void upgrade() {
        this.indexSetService.findAll()
            .stream()
            .map(mongoIndexSetFactory::create)
            .flatMap(indexSet -> getReopenedIndices(indexSet).stream())
            .forEach(indices::markIndexReopened);
    }

    private Set<String> getReopenedIndices(final Collection<String> indices) {
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

    private boolean checkForReopened(@Nullable JsonObject indexSettings) {
        return Optional.ofNullable(indexSettings)
            .map(settings -> {
                if (elasticsearchVersion.satisfies(">=2.1.0 & <5.0.0")) {
                    return settings;
                } else if (elasticsearchVersion.satisfies("^5.0.0")) {
                    return asJsonObject(settings.get("archived"));
                } else {
                    throw new ElasticsearchException("Unsupported Elasticsearch version: " + elasticsearchVersion);
                }
            })
            .map(settings -> asString(settings.get(REOPENED_INDEX_SETTING))) // WTF, why is this a string?
            .map(Boolean::parseBoolean)
            .orElse(false);
    }

    private Set<String> getReopenedIndices(final IndexSet indexSet) {
        return getReopenedIndices(Collections.singleton(indexSet.getIndexWildcard()));
    }

    private JsonObject getClusterStateIndicesMetadata(JsonObject clusterStateJson) {
        return Optional.ofNullable(clusterStateJson)
            .map(json -> asJsonObject(json.get("metadata")))
            .map(metadata -> asJsonObject(metadata.get("indices")))
            .orElse(new JsonObject());
    }

    private JsonObject getIndexSettings(JsonObject indicesJson, String index) {
        return Optional.ofNullable(asJsonObject(indicesJson))
            .map(indices -> asJsonObject(indices.get(index)))
            .map(idx -> asJsonObject(idx.get("settings")))
            .map(settings -> asJsonObject(settings.get("index")))
            .orElse(new JsonObject());
    }
}

