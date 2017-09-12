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
package org.graylog2.indexer.results;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.joschi.nosqlunit.elasticsearch.http.ElasticsearchConfiguration;
import com.google.common.collect.ImmutableMap;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.params.Parameters;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog2.ElasticsearchBase;
import org.graylog2.indexer.IndexMapping;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.TestIndexSet;
import org.graylog2.indexer.cluster.jest.JestUtils;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

public class ScrollResultIT extends ElasticsearchBase {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private static final String INDEX_NAME = "graylog_0";

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Override
    protected ElasticsearchConfiguration.Builder elasticsearchConfiguration() {
        final Map<String, Map<String, Object>> messageTemplates = Collections.singletonMap("graylog-test-internal", indexMapping().messageTemplate("*", "standard"));
        final IndexSetConfig indexSetConfig = IndexSetConfig.builder()
                .id("index-set-1")
                .title("Index set 1")
                .description("For testing")
                .indexPrefix("graylog")
                .creationDate(ZonedDateTime.now())
                .shards(1)
                .replicas(0)
                .rotationStrategyClass(MessageCountRotationStrategy.class.getCanonicalName())
                .rotationStrategy(MessageCountRotationStrategyConfig.createDefault())
                .retentionStrategyClass(DeletionRetentionStrategy.class.getCanonicalName())
                .retentionStrategy(DeletionRetentionStrategyConfig.createDefault())
                .indexAnalyzer("standard")
                .indexTemplateName("template-1")
                .indexOptimizationMaxNumSegments(1)
                .indexOptimizationDisabled(false)
                .build();
        final IndexSet indexSet = new TestIndexSet(indexSetConfig);
        final Map<String, Object> indexSettings = ImmutableMap.of("settings", ImmutableMap.of(
                "number_of_shards", indexSet.getConfig().shards(),
                "number_of_replicas", indexSet.getConfig().replicas()
        ));
        return super.elasticsearchConfiguration()
                .indexTemplates(messageTemplates)
                .createIndices(false)
                .indexSettings(indexSettings)
                .deleteAllIndices(true);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void nextChunkDoesNotContainJestMetadata() throws IOException {
        final String query = SearchSourceBuilder.searchSource().query(matchAllQuery()).toString();
        final Search request = new Search.Builder(query)
                .addIndex(INDEX_NAME)
                .addType(IndexMapping.TYPE_MESSAGE)
                .setParameter(Parameters.SCROLL, "1m")
                .setParameter(Parameters.SIZE, 5)
                .build();
        final SearchResult searchResult = JestUtils.execute(client(), request, () -> "Exception");

        assertThat(client()).isNotNull();
        final ScrollResult scrollResult = new ScrollResult(client(), objectMapper, searchResult, "*", Collections.singletonList("message"));
        scrollResult.nextChunk().getMessages().forEach(
                message -> assertThat(message.getMessage().getFields()).doesNotContainKeys("es_metadata_id", "es_metadata_version")
        );
        scrollResult.nextChunk().getMessages().forEach(
                message -> assertThat(message.getMessage().getFields()).doesNotContainKeys("es_metadata_id", "es_metadata_version")
        );
        assertThat(scrollResult.nextChunk()).isNull();
    }
}