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
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.graylog2.AbstractESTest;
import org.graylog2.audit.NullAuditEventSender;
import org.graylog2.indexer.IndexMappingFactory;
import org.graylog2.indexer.cluster.Node;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.plugin.system.NodeId;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class IndicesGetAllMessageFieldsTest extends AbstractESTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private Indices indices;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        final Node node = new Node(jestClient());
        indices = new Indices(jestClient(),
                new ObjectMapper(),
                new IndexMappingFactory(node),
                new Messages(new MetricRegistry(), jestClient()),
                mock(NodeId.class),
                new NullAuditEventSender(),
                new EventBus());
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void GetAllMessageFieldsForNonexistingIndexShouldReturnEmptySet() throws Exception {
        final String[] indexNames = new String[] { "graylog_0" };
        final Set<String> result = indices.getAllMessageFields(indexNames);

        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    @UsingDataSet(locations = "IndicesTest-EmptyIndex.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void GetAllMessageFieldsForEmptyIndexShouldReturnEmptySet() throws Exception {
        final String[] indexNames = new String[] { "graylog_0" };
        final Set<String> result = indices.getAllMessageFields(indexNames);

        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    @UsingDataSet(locations = "IndicesGetAllMessageFieldsTest-MultipleIndices.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void GetAllMessageFieldsForSingleIndexShouldReturnCompleteList() throws Exception {
        final String[] indexNames = new String[] { "graylog_0" };
        final Set<String> result = indices.getAllMessageFields(indexNames);

        assertThat(result)
            .isNotNull()
            .hasSize(5)
            .containsOnly("fieldonlypresenthere", "message", "n", "source", "timestamp");

        final String[] otherIndexName = new String[] { "otherindexset_0" };

        final Set<String> otherResult = indices.getAllMessageFields(otherIndexName);

        assertThat(otherResult)
            .isNotNull()
            .hasSize(5)
            .containsOnly("message", "n", "source", "timestamp", "someotherfield");
    }

    @Test
    @UsingDataSet(locations = "IndicesGetAllMessageFieldsTest-MultipleIndices.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void GetAllMessageFieldsForMultipleIndicesShouldReturnCompleteList() throws Exception {
        final String[] indexNames = new String[] { "graylog_0", "otherindexset_0" };
        final Set<String> result = indices.getAllMessageFields(indexNames);

        assertThat(result)
            .isNotNull()
            .hasSize(6)
            .containsOnly("message", "n", "source", "timestamp", "fieldonlypresenthere", "someotherfield");
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void GetAllMessageFieldsForIndicesForNonexistingIndexShouldReturnEmptySet() throws Exception {
        final String[] indexNames = new String[] { "graylog_0" };
        final IndicesExistsResponse response = client().admin().indices().exists(new IndicesExistsRequest("graylog_0")).get();
        assertThat(response.isExists()).isFalse();
        final Map<String, Set<String>> result = indices.getAllMessageFieldsForIndices(indexNames);

        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    @UsingDataSet(locations = "IndicesTest-EmptyIndex.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void GetAllMessageFieldsForIndicesForEmptyIndexShouldReturnEmptySet() throws Exception {
        final String[] indexNames = new String[] { "graylog_0" };
        final Map<String, Set<String>> result = indices.getAllMessageFieldsForIndices(indexNames);

        assertThat(result)
            .isNotNull()
            .isEmpty();
    }

    @Test
    @UsingDataSet(locations = "IndicesGetAllMessageFieldsTest-MultipleIndices.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void GetAllMessageFieldsForIndicesForSingleIndexShouldReturnCompleteList() throws Exception {
        final String indexName = "graylog_0";
        final String[] indexNames = new String[] { indexName };
        final Map<String, Set<String>> result = indices.getAllMessageFieldsForIndices(indexNames);

        assertThat(result)
            .isNotNull()
            .hasSize(1)
            .containsOnlyKeys(indexName);

        assertThat(result.get(indexName))
            .isNotNull()
            .hasSize(5)
            .containsOnly("fieldonlypresenthere", "message", "n", "source", "timestamp");

        final String otherIndexName = "otherindexset_0";
        final String[] otherIndexNames = new String[] { otherIndexName };

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
    @UsingDataSet(locations = "IndicesGetAllMessageFieldsTest-MultipleIndices.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void GetAllMessageFieldsForIndicesForMultipleIndicesShouldReturnCompleteList() throws Exception {
        final String[] indexNames = new String[] { "graylog_0", "otherindexset_0" };
        final Map<String, Set<String>> result = indices.getAllMessageFieldsForIndices(indexNames);

        assertThat(result)
            .isNotNull()
            .hasSize(2)
            .containsOnlyKeys("graylog_0", "otherindexset_0");

        assertThat(result.get("graylog_0"))
            .isNotNull()
            .hasSize(5)
            .containsOnly("message", "n", "source", "timestamp", "fieldonlypresenthere");

        assertThat(result.get("otherindexset_0"))
            .isNotNull()
            .hasSize(5)
            .containsOnly("message", "n", "source", "timestamp", "someotherfield");
    }

    @After
    public void tearDown() throws Exception {
        final GetIndexResponse response = client().admin().indices().getIndex(new GetIndexRequest()).get();
        for (String index : response.indices()) {
            client().admin().indices().delete(new DeleteIndexRequest(index)).get();
        }
    }
}
