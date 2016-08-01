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
package org.graylog2.rest.documentation.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.graylog2.shared.ServerVersion;
import org.graylog2.shared.rest.documentation.generator.Generator;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class GeneratorTest {

    static ObjectMapper objectMapper;

    @BeforeClass
    public static void init() {
        objectMapper = new ObjectMapper();
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    }

    @Test
    public void testGenerateOverview() throws Exception {
        Generator generator = new Generator("org.graylog2.rest.resources", objectMapper);
        Map<String, Object> result = generator.generateOverview();

        assertEquals(ServerVersion.VERSION.toString(), result.get("apiVersion"));
        assertEquals(Generator.EMULATED_SWAGGER_VERSION, result.get("swaggerVersion"));

        assertNotNull(result.get("apis"));
        assertTrue(((List) result.get("apis")).size() > 0);
    }

    @Test
    public void testGenerateForRoute() throws Exception {
        Generator generator = new Generator("org.graylog2.rest.resources", objectMapper);
        Map<String, Object> result = generator.generateForRoute("/system", "http://localhost:12900/");
    }

}
