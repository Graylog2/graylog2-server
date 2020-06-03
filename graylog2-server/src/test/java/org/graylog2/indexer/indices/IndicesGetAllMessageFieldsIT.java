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

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import org.graylog.testing.elasticsearch.ElasticsearchBaseTest;
import org.graylog.testing.elasticsearch.SkipDefaultIndexTemplate;
import org.graylog2.audit.NullAuditEventSender;
import org.graylog2.indexer.IndexMappingFactory;
import org.graylog2.indexer.cluster.Node;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.indexer.messages.MessagesAdapter;
import org.graylog2.plugin.system.NodeId;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class IndicesGetAllMessageFieldsIT extends ElasticsearchBaseTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private MessagesAdapter messagesAdapter;

    private Indices indices;

    @Before
    public void setUp() throws Exception {
        final Node node = new Node(jestClient());
        //noinspection UnstableApiUsage
        indices = new Indices(jestClient(),
                new ObjectMapper(),
                new IndexMappingFactory(node),
                new Messages(new MetricRegistry(), messagesAdapter),
                mock(NodeId.class),
                new NullAuditEventSender(),
                new EventBus());
    }

    @Test
    public void GetAllMessageFieldsForNonexistingIndexShouldReturnEmptySet() {
        final String[] indexNames = new String[]{"does_not_exist_" + System.nanoTime()};
        final Set<String> result = indices.getAllMessageFields(indexNames);

        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    @SkipDefaultIndexTemplate
    public void GetAllMessageFieldsForEmptyIndexShouldReturnEmptySet() {
        final String indexName = client().createRandomIndex("get_all_message_fields_");

        final Set<String> result = indices.getAllMessageFields(new String[]{indexName});
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    @SkipDefaultIndexTemplate
    public void GetAllMessageFieldsForSingleIndexShouldReturnCompleteList() {
        importFixture("IndicesGetAllMessageFieldsIT-MultipleIndices.json");

        final String[] indexNames = new String[]{"get_all_message_fields_0"};
        final Set<String> result = indices.getAllMessageFields(indexNames);

        assertThat(result)
                .isNotNull()
                .hasSize(5)
                .containsOnly("fieldonlypresenthere", "message", "n", "source", "timestamp");

        final String[] otherIndexName = new String[]{"get_all_message_fields_1"};

        final Set<String> otherResult = indices.getAllMessageFields(otherIndexName);

        assertThat(otherResult)
                .isNotNull()
                .hasSize(5)
                .containsOnly("message", "n", "source", "timestamp", "someotherfield");
    }

    @Test
    @SkipDefaultIndexTemplate
    public void GetAllMessageFieldsForMultipleIndicesShouldReturnCompleteList() {
        importFixture("IndicesGetAllMessageFieldsIT-MultipleIndices.json");

        final String[] indexNames = new String[]{"get_all_message_fields_0", "get_all_message_fields_1"};
        final Set<String> result = indices.getAllMessageFields(indexNames);

        assertThat(result)
                .isNotNull()
                .hasSize(6)
                .containsOnly("message", "n", "source", "timestamp", "fieldonlypresenthere", "someotherfield");
    }

    @Test
    public void GetAllMessageFieldsForIndicesForNonexistingIndexShouldReturnEmptySet() {
        final String indexName = "does_not_exist_" + System.nanoTime();
        assertThat(client().indicesExists(indexName)).isFalse();

        final Map<String, Set<String>> result = indices.getAllMessageFieldsForIndices(new String[]{indexName});

        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    @SkipDefaultIndexTemplate
    public void GetAllMessageFieldsForIndicesForEmptyIndexShouldReturnEmptySet() {
        final String indexName = client().createRandomIndex("indices_it_");

        final Map<String, Set<String>> result = indices.getAllMessageFieldsForIndices(new String[]{indexName});

        assertThat(result)
                .isNotNull()
                .isEmpty();
    }

    @Test
    @SkipDefaultIndexTemplate
    public void GetAllMessageFieldsForIndicesForSingleIndexShouldReturnCompleteList() {
        importFixture("IndicesGetAllMessageFieldsIT-MultipleIndices.json");

        final String indexName = "get_all_message_fields_0";
        final String[] indexNames = new String[]{indexName};
        final Map<String, Set<String>> result = indices.getAllMessageFieldsForIndices(indexNames);

        assertThat(result)
                .isNotNull()
                .hasSize(1)
                .containsOnlyKeys(indexName);

        assertThat(result.get(indexName))
                .isNotNull()
                .hasSize(5)
                .containsOnly("fieldonlypresenthere", "message", "n", "source", "timestamp");

        final String otherIndexName = "get_all_message_fields_1";
        final String[] otherIndexNames = new String[]{otherIndexName};

        final Map<String, Set<String>> otherResult = indices.getAllMessageFieldsForIndices(otherIndexNames);

        assertThat(otherResult)
                .isNotNull()
                .hasSize(1)
                .containsOnlyKeys(otherIndexName);

        assertThat(otherResult.get(otherIndexName))
                .isNotNull()
                .hasSize(5)
                .containsOnly("someotherfield", "message", "n", "source", "timestamp");
    }

    @Test
    @SkipDefaultIndexTemplate
    public void GetAllMessageFieldsForIndicesForMultipleIndicesShouldReturnCompleteList() {
        importFixture("IndicesGetAllMessageFieldsIT-MultipleIndices.json");

        final String[] indexNames = new String[]{"get_all_message_fields_0", "get_all_message_fields_1"};
        final Map<String, Set<String>> result = indices.getAllMessageFieldsForIndices(indexNames);

        assertThat(result)
                .isNotNull()
                .hasSize(2)
                .containsOnlyKeys("get_all_message_fields_0", "get_all_message_fields_1");

        assertThat(result.get("get_all_message_fields_0"))
                .isNotNull()
                .hasSize(5)
                .containsOnly("message", "n", "source", "timestamp", "fieldonlypresenthere");

        assertThat(result.get("get_all_message_fields_1"))
                .isNotNull()
                .hasSize(5)
                .containsOnly("message", "n", "source", "timestamp", "someotherfield");
    }
}
