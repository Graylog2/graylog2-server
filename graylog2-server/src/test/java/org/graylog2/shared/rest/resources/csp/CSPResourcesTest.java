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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CSPResourcesTest {
    static CSPResources cspResources;

    @BeforeEach
    void setup() {
        cspResources = new CSPResources("/org/graylog2/security/cspTest.config");
    }

    @Test
    void loadPropertiesTest() {
        assertThat(cspResources.cspString("default")).isEqualTo(
                "connect-src url1.com:9999 url2.com;default-src 'self';img-src https://url3.com:9999 https://url4.com:9999;script-src 'self' 'unsafe-eval';style-src 'self' 'unsafe-inline'");
        assertThat(cspResources.cspString("swagger")).isEqualTo(
                "connect-src url4.com;img-src https://url5.com:9999;script-src 'self' 'unsafe-eval' 'unsafe-inline';style-src 'self' 'unsafe-inline'");
    }

    @Test
    void updateTest() {
        cspResources.updateAll("default-src", "xxx xxx yyy yyy");
        assertThat(cspResources.cspString("default")).isEqualTo(
                "connect-src url1.com:9999 url2.com;default-src 'self' xxx yyy;img-src https://url3.com:9999 https://url4.com:9999;script-src 'self' 'unsafe-eval';style-src 'self' 'unsafe-inline'");
        assertThat(cspResources.cspString("swagger")).isEqualTo(
                "connect-src url4.com;default-src xxx yyy;img-src https://url5.com:9999;script-src 'self' 'unsafe-eval' 'unsafe-inline';style-src 'self' 'unsafe-inline'");
    }
}
