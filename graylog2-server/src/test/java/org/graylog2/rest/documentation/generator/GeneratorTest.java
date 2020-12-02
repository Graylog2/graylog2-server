/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
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
