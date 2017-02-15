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
package org.graylog2.configuration;

import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.repositories.InMemoryRepository;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

public class ElasticsearchConfigurationTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testGetElasticSearchIndexPrefix() throws RepositoryException, ValidationException {
        ElasticsearchConfiguration configuration = new ElasticsearchConfiguration();
        new JadConfig(new InMemoryRepository(), configuration).process();

        assertEquals(configuration.getIndexPrefix(), "graylog");
    }

    @Test
    public void testGetPathData() throws ValidationException, RepositoryException {
        final ElasticsearchConfiguration configuration = new ElasticsearchConfiguration();
        new JadConfig(new InMemoryRepository(), configuration).process();

        assertEquals(configuration.getPathData(), "data/elasticsearch");
    }

    @Test
    public void testGetPathHome() throws ValidationException, RepositoryException {
        final ElasticsearchConfiguration configuration = new ElasticsearchConfiguration();
        new JadConfig(new InMemoryRepository(), configuration).process();

        assertEquals(configuration.getPathHome(), "data/elasticsearch");
    }

    @Test
    public void testIsClientNode() throws ValidationException, RepositoryException {
        final Map<String, String> props = new HashMap<>();
        final ElasticsearchConfiguration configuration1 = new ElasticsearchConfiguration();

        new JadConfig(new InMemoryRepository(), configuration1).process();

        assertTrue(configuration1.isClientNode());

        final ElasticsearchConfiguration configuration2 = new ElasticsearchConfiguration();
        props.put("elasticsearch_node_data", "false");
        new JadConfig(new InMemoryRepository(props), configuration2).process();

        assertTrue(configuration2.isClientNode());

        final ElasticsearchConfiguration configuration3 = new ElasticsearchConfiguration();
        props.put("elasticsearch_node_data", "true");
        new JadConfig(new InMemoryRepository(props), configuration3).process();

        assertFalse(configuration3.isClientNode());
    }

    @Test(expected = ValidationException.class)
    public void throwValidationExceptionIfHomePathIsNotReadable() throws Exception {
        final File path = temporaryFolder.newFolder("elasticsearch-home");
        assumeTrue(path.setReadable(false));

        final ElasticsearchConfiguration configuration = new ElasticsearchConfiguration() {
            @Override
            public String getPathHome() {
                return path.getAbsolutePath();
            }
        };
        new JadConfig(new InMemoryRepository(), configuration).process();
    }

    @Test(expected = ValidationException.class)
    public void throwValidationExceptionIfDataPathIsNotReadable() throws Exception {
        final File path = temporaryFolder.newFolder("elasticsearch-data");
        assumeTrue(path.setReadable(false));

        final ElasticsearchConfiguration configuration = new ElasticsearchConfiguration() {
            @Override
            public String getPathData() {
                return path.getAbsolutePath();
            }
        };
        new JadConfig(new InMemoryRepository(), configuration).process();
    }

    @Test(expected = ValidationException.class)
    public void throwValidationExceptionIfHomePathParentIsNotReadable() throws Exception {
        final File parent = temporaryFolder.newFolder("elasticsearch");
        final File path = new File(parent, "home");
        assumeTrue(path.mkdir());
        assumeTrue(parent.setReadable(false));

        final ElasticsearchConfiguration configuration = new ElasticsearchConfiguration() {
            @Override
            public String getPathHome() {
                return path.getAbsolutePath();
            }
        };
        new JadConfig(new InMemoryRepository(), configuration).process();
    }

    @Test(expected = ValidationException.class)
    public void throwValidationExceptionIfDataPathParentIsNotReadable() throws Exception {
        final File parent = temporaryFolder.newFolder("elasticsearch");
        final File path = new File(parent, "data");
        assumeTrue(path.mkdir());
        assumeTrue(parent.setReadable(false));

        final ElasticsearchConfiguration configuration = new ElasticsearchConfiguration() {
            @Override
            public String getPathData() {
                return path.getAbsolutePath();
            }
        };
        new JadConfig(new InMemoryRepository(), configuration).process();
    }

    @Test(expected = ValidationException.class)
    public void throwValidationExceptionIfHomePathIsNotADirectory() throws Exception {
        final File path = temporaryFolder.newFile("elasticsearch-home");

        final ElasticsearchConfiguration configuration = new ElasticsearchConfiguration() {
            @Override
            public String getPathHome() {
                return path.getAbsolutePath();
            }
        };
        new JadConfig(new InMemoryRepository(), configuration).process();
    }

    @Test(expected = ValidationException.class)
    public void throwValidationExceptionIfDataPathIsNotADirectory() throws Exception {
        final File path = temporaryFolder.newFile("elasticsearch-data");

        final ElasticsearchConfiguration configuration = new ElasticsearchConfiguration() {
            @Override
            public String getPathData() {
                return path.getAbsolutePath();
            }
        };
        new JadConfig(new InMemoryRepository(), configuration).process();
    }
}
