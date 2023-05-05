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
package org.graylog2.shared.rest.resources.csp;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CSPResourcesTest {
    final static String DEFAULT_DIR = "connect-src *;default-src 'self';img-src *;script-src 'self' 'unsafe-eval';style-src 'self' 'unsafe-inline'";
    static CSPResources cspResources;

    @BeforeAll
    static void setup() {
        cspResources = new CSPResources();
    }

    @Test
    void loadPropertiesTest() {
        assertThat(cspResources.cspString("default")).isEqualTo(DEFAULT_DIR);
    }

    @Test
    void mergeTest() {
        String csp1 = "default-src v1 v2;img-src v3 v4";
        String expected = "default-src v1 v2 'self';img-src v3 v4 *;script-src 'self' 'unsafe-eval';style-src 'self' 'unsafe-inline';connect-src *;";
        ;
        assertThat(cspResources.merge(csp1, "default")).isEqualTo(expected);
    }
}
