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
package org.graylog2.utilities;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.revinate.assertj.json.JsonPathAssert;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;

import java.util.function.Consumer;

public class AssertJsonPath {
    private static final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private static final Configuration configuration = Configuration.builder()
            .mappingProvider(new JacksonMappingProvider(objectMapper))
            .jsonProvider(new JacksonJsonProvider(objectMapper))
            .build();


    public static void assertJsonPath(Object obj, Consumer<JsonPathAssert> consumer) {
        assertJsonPath(obj.toString(), consumer);
    }

    public static void assertJsonPath(String json, Consumer<JsonPathAssert> consumer) {
        final DocumentContext context = JsonPath.parse(json, configuration);
        final JsonPathAssert jsonPathAssert = JsonPathAssert.assertThat(context);

        consumer.accept(jsonPathAssert);
    }
}
