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
package org.graylog2.indexer;

import com.github.zafarkhaja.semver.Version;
import org.graylog2.indexer.cluster.Node;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.when;

public class IndexMappingFactoryTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private Node node;

    private IndexMappingFactory indexMappingFactory;

    @Before
    public void setUp() throws Exception {
        this.indexMappingFactory = new IndexMappingFactory(node);
    }

    @Test
    public void createIndexMappingFailsIfElasticsearch1VersionIsTooLow() throws Exception {
        when(node.getVersion()).thenReturn(Optional.of(Version.valueOf("1.7.3")));

        assertThatThrownBy(indexMappingFactory::createIndexMapping)
                .isInstanceOf(ElasticsearchException.class)
                .hasMessageStartingWith("Unsupported Elasticsearch version: 1.7.3")
                .hasNoCause();
    }

    @Test
    public void createIndexMappingFailsIfElasticsearch2VersionIsTooLow() throws Exception {
        when(node.getVersion()).thenReturn(Optional.of(Version.valueOf("2.0.0")));

        assertThatThrownBy(indexMappingFactory::createIndexMapping)
                .isInstanceOf(ElasticsearchException.class)
                .hasMessageStartingWith("Unsupported Elasticsearch version: 2.0.0")
                .hasNoCause();
    }

    @Test
    public void createIndexMappingFailsIfElasticsearch6VersionIsTooHigh() throws Exception {
        when(node.getVersion()).thenReturn(Optional.of(Version.valueOf("6.0.0")));

        assertThatThrownBy(indexMappingFactory::createIndexMapping)
                .isInstanceOf(ElasticsearchException.class)
                .hasMessageStartingWith("Unsupported Elasticsearch version: 6.0.0")
                .hasNoCause();
    }

    @RunWith(Parameterized.class)
    public static class ParameterizedTest {
        @Parameterized.Parameters
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {"5.0.0", IndexMapping5.class},
                    {"5.1.0", IndexMapping5.class},
                    {"5.2.0", IndexMapping5.class},
                    {"5.3.0", IndexMapping5.class},
                    {"5.4.0", IndexMapping5.class},
            });
        }

        @Rule
        public final MockitoRule mockitoRule = MockitoJUnit.rule();

        private final String version;
        private final Class<? extends IndexMapping> expectedMapping;

        @Mock
        private Node node;

        private IndexMappingFactory indexMappingFactory;


        public ParameterizedTest(String version, Class<? extends IndexMapping> expectedMapping) {
            this.version = version;
            this.expectedMapping = expectedMapping;
        }

        @Before
        public void setUp() throws Exception {
            when(node.getVersion()).thenReturn(Optional.of(Version.valueOf(this.version)));
            indexMappingFactory = new IndexMappingFactory(node);
        }

        @Test
        public void test() throws Exception {
            assertThat(indexMappingFactory.createIndexMapping()).isInstanceOf(expectedMapping);
        }
    }
}
