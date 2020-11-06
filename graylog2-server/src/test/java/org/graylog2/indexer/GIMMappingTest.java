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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.glassfish.grizzly.utils.Charsets;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

abstract class GIMMappingTest {
    private static final ObjectMapper mapper = new ObjectMapperProvider().get();

    String json(Object object) throws JsonProcessingException {
        return mapper.writeValueAsString(object);
    }

    IndexSetConfig mockIndexSetConfig() {
        final IndexSetConfig indexSetConfig = mock(IndexSetConfig.class);
        when(indexSetConfig.indexAnalyzer()).thenReturn("standard");

        return indexSetConfig;
    }

    String resource(String filename) throws IOException {
        return Resources.toString(Resources.getResource(this.getClass(), filename), Charsets.UTF8_CHARSET);
    }

    @Test
    void matchesJsonSource() throws Exception {
        final IndexMappingTemplate template = createTemplate();
        final IndexSetConfig indexSetConfig = mockIndexSetConfig();

        final Map<String, Object> result = template.toTemplate(indexSetConfig, "myindex*", -2147483648);

        assertEquals(expectedGimTemplate(), json(result), true);
    }

    abstract IndexMappingTemplate createTemplate();

    abstract String expectedGimTemplate() throws IOException;
}
