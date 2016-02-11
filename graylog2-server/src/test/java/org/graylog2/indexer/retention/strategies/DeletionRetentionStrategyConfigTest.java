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

package org.graylog2.indexer.retention.strategies;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import org.graylog2.plugin.indexer.retention.RetentionStrategyConfig;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class DeletionRetentionStrategyConfigTest {
    @Test
    public void testCreate() throws Exception {
        final DeletionRetentionStrategyConfig config = DeletionRetentionStrategyConfig.create(21);

        assertThat(config.maxNumberOfIndices()).isEqualTo(21);
    }

    @Test
    public void testSerialization() throws JsonProcessingException {
        final DeletionRetentionStrategyConfig config = DeletionRetentionStrategyConfig.create(25);
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        final String json = objectMapper.writeValueAsString(config);

        final Object document = Configuration.defaultConfiguration().jsonProvider().parse(json);
        assertThat((String) JsonPath.read(document, "$.type")).isEqualTo("org.graylog2.indexer.retention.strategies.DeletionRetentionStrategyConfig");
        assertThat((Integer) JsonPath.read(document, "$.max_number_of_indices")).isEqualTo(25);
    }

    @Test
    public void testDeserialization() throws IOException {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        final String json = "{ \"type\": \"org.graylog2.indexer.retention.strategies.DeletionRetentionStrategyConfig\", \"max_number_of_indices\": 23}";
        final RetentionStrategyConfig config = objectMapper.readValue(json, RetentionStrategyConfig.class);

        assertThat(config).isInstanceOf(DeletionRetentionStrategyConfig.class);
        assertThat(((DeletionRetentionStrategyConfig) config).maxNumberOfIndices()).isEqualTo(23);
    }
}