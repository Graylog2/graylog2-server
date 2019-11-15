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
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.params.Parameters;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog.testing.elasticsearch.ElasticsearchBaseTest;
import org.graylog2.indexer.IndexMapping;
import org.graylog2.indexer.cluster.jest.JestUtils;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

public class ScrollResultIT extends ElasticsearchBaseTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private static final String INDEX_NAME = "graylog_0";

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Test
    public void nextChunkDoesNotContainJestMetadata() throws IOException {
        importFixture("ScrollResultIT.json");

        final String query = SearchSourceBuilder.searchSource().query(matchAllQuery()).toString();
        final Search request = new Search.Builder(query)
                .addIndex(INDEX_NAME)
                .addType(IndexMapping.TYPE_MESSAGE)
                .setParameter(Parameters.SCROLL, "1m")
                .setParameter(Parameters.SIZE, 5)
                .build();
        final SearchResult searchResult = JestUtils.execute(jestClient(), request, () -> "Exception");

        assertThat(jestClient()).isNotNull();
        final ScrollResult scrollResult = new ScrollResult(jestClient(), objectMapper, searchResult, "*", Collections.singletonList("message"));
        scrollResult.nextChunk().getMessages().forEach(
                message -> assertThat(message.getMessage().getFields()).doesNotContainKeys("es_metadata_id", "es_metadata_version")
        );
        scrollResult.nextChunk().getMessages().forEach(
                message -> assertThat(message.getMessage().getFields()).doesNotContainKeys("es_metadata_id", "es_metadata_version")
        );
        assertThat(scrollResult.nextChunk()).isNull();
    }
}
