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
package org.graylog2.migrations;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.zafarkhaja.semver.Version;
import com.google.common.collect.ImmutableSet;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class V20170607164210_MigrateReopenedIndicesToAliases extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20170607164210_MigrateReopenedIndicesToAliases.class);
    private static final String REOPENED_INDEX_SETTING = "graylog2_reopened";

    private Node node;
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
        this.node = node;
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
            .map(indexName -> { LOG.debug("Marking index {} to be reopened using alias.", indexName); return indexName; })
            .forEach(indices::markIndexReopened);
    }

    private Set<String> getReopenedIndices(final Collection<String> indices) {
        final Version elasticsearchVersion = node.getVersion().orElseThrow(() -> new ElasticsearchException("Unable to retrieve Elasticsearch version."));
        final String indexList = String.join(",", indices);
        final State request = new State.Builder().withMetadata().indices(indexList).build();

        final JestResult jestResult = JestUtils.execute(jestClient, request, () -> "Couldn't read cluster state for reopened indices " + indices);
        final JsonNode clusterStateJson = jestResult.getJsonObject();
        final JsonNode indicesJson = clusterStateJson.path("metadata").path("indices");

        final ImmutableSet.Builder<String> reopenedIndices = ImmutableSet.builder();

        if (indicesJson.isMissingNode()) {
            LOG.error("Retrieved cluster state is invalid (no metadata.indices key).");
            LOG.debug("Received cluster state was: {}", clusterStateJson.toString());
            return Collections.emptySet();
        }

        for (Iterator<Map.Entry<String, JsonNode>> it = indicesJson.fields(); it.hasNext(); ) {
            final Map.Entry<String, JsonNode> entry = it.next();
            final String indexName = entry.getKey();
            final JsonNode value = entry.getValue();

            final JsonNode indexSettings = value.path("settings");
            if (indexSettings.isMissingNode()) {
                LOG.error("Unable to retrieve index settings from metadata for index {} - skipping.", indexName);
                LOG.debug("Index metadata was: {}", value.toString());
                continue;
            }
            if (checkForReopened(indexSettings, elasticsearchVersion)) {
                LOG.debug("Adding {} to list of indices to be migrated.", indexName);
                reopenedIndices.add(indexName);
            }
        }

        return reopenedIndices.build();
    }

    private boolean checkForReopened(@Nullable JsonNode indexSettings, Version elasticsearchVersion) {
        final JsonNode settings;
        if (elasticsearchVersion.satisfies(">=2.1.0 & <5.0.0")) {
            settings = indexSettings;
        } else if (elasticsearchVersion.satisfies("^5.0.0")) {
            settings = indexSettings.path("archived");
        } else {
            throw new ElasticsearchException("Unsupported Elasticsearch version: " + elasticsearchVersion);
        }

        final JsonNode reopened = settings.path("index").path(REOPENED_INDEX_SETTING);
        return reopened.isMissingNode() ? false : reopened.asBoolean();
    }

    private Set<String> getReopenedIndices(final IndexSet indexSet) {
        return getReopenedIndices(Collections.singleton(indexSet.getIndexWildcard()));
    }
}

