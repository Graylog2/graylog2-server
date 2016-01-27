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
package org.graylog2.indexer.counts;

import com.github.joschi.nosqlunit.elasticsearch2.ElasticsearchRule;
import com.github.joschi.nosqlunit.elasticsearch2.EmbeddedElasticsearch;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.graylog2.indexer.Deflector;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.inject.Inject;

import static com.github.joschi.nosqlunit.elasticsearch2.ElasticsearchRule.ElasticsearchRuleBuilder.newElasticsearchRule;
import static com.github.joschi.nosqlunit.elasticsearch2.EmbeddedElasticsearch.EmbeddedElasticsearchRuleBuilder.newEmbeddedElasticsearchRule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.when;

public class CountsTest {
    private static final String INDEX_NAME = "counts_test";
    @ClassRule
    public static final EmbeddedElasticsearch EMBEDDED_ELASTICSEARCH = newEmbeddedElasticsearchRule().build();
    @Rule
    public ElasticsearchRule elasticsearchRule = newElasticsearchRule().defaultEmbeddedElasticsearch();
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Inject
    private Client client;

    @Mock
    private Deflector deflector;
    private Counts counts;

    @Before
    public void setUp() throws Exception {
        final CreateIndexResponse createIndexResponse = client.admin().indices()
                .prepareCreate(INDEX_NAME)
                .setTimeout(TimeValue.timeValueSeconds(10L))
                .execute()
                .get();
        assumeTrue(createIndexResponse.isAcknowledged());

        when(deflector.getAllDeflectorIndexNames()).thenReturn(new String[]{INDEX_NAME});

        counts = new Counts(client, deflector);
    }

    @After
    public void tearDown() throws Exception {
        final DeleteIndexResponse deleteIndexResponse = client.admin().indices()
                .prepareDelete(INDEX_NAME)
                .setTimeout(TimeValue.timeValueSeconds(10L))
                .execute()
                .get();
        assumeTrue(deleteIndexResponse.isAcknowledged());
    }

    @Test
    public void totalReturnsZeroWithEmptyIndex() throws Exception {
        assertThat(counts.total()).isEqualTo(0L);
    }

    @Test
    public void totalReturnsNumberOfMessages() throws Exception {
        final int count = 10;
        for (int i = 0; i < count; i++) {
            final IndexResponse indexResponse = client.prepareIndex()
                    .setIndex(INDEX_NAME)
                    .setRefresh(true)
                    .setType("test")
                    .setSource("foo", "bar", "counter", i)
                    .execute().get();
            assumeTrue(indexResponse.isCreated());
        }

        assertThat(counts.total()).isEqualTo(count);
    }
}