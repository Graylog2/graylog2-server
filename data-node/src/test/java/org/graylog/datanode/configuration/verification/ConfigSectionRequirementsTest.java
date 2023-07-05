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
package org.graylog.datanode.configuration.verification;


import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigSectionRequirementsTest {

    @Test
    void testReplacesNullValuesWithEmptyCollectionsDuringConstruction() {
        ConfigSectionRequirements toTest = new ConfigSectionRequirements(null, null);

        assertNotNull(toTest.requiredStringProperties());
        assertNotNull(toTest.requiredFiles());
        assertTrue(toTest.requiredFiles().isEmpty());
        assertTrue(toTest.requiredStringProperties().isEmpty());
    }

    @Test
    void testRequirementCounting() {
        assertEquals(0, new ConfigSectionRequirements(null, null).requirementsCount());
        assertEquals(0, new ConfigSectionRequirements(List.of(), null).requirementsCount());
        assertEquals(0, new ConfigSectionRequirements(null, List.of()).requirementsCount());
        assertEquals(1, new ConfigSectionRequirements(null, List.of(Path.of("whatever"))).requirementsCount());
        assertEquals(1, new ConfigSectionRequirements(
                List.of(new ConfigProperty("importantProperty", "whatever")),
                List.of()).requirementsCount());
        assertEquals(3, new ConfigSectionRequirements(
                List.of(new ConfigProperty("importantProperty", "whatever"),
                        new ConfigProperty("importantProperty2", "smthelse")),
                List.of(Path.of("secret.file"))).requirementsCount());
    }

    @Test
    void testRequirementList() {
        final List<String> requirementsList = new ConfigSectionRequirements(
                List.of(new ConfigProperty("importantProperty", "whatever"),
                        new ConfigProperty("importantProperty2", "smthelse")),
                List.of(Path.of("secret.file"))).requirementsList();

        assertEquals(3, requirementsList.size());
        assertTrue(requirementsList.get(0).startsWith("importantProperty"));
        assertTrue(requirementsList.get(1).startsWith("importantProperty2"));
        assertTrue(requirementsList.get(2).contains("secret.file"));
    }
}
