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
package org.graylog.plugins.views.aggregations;

import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.completebackend.apis.inputs.PortBoundGelfInputApi;
import org.graylog.testing.completebackend.FullBackendTest;
import org.graylog.testing.completebackend.GraylogBackendConfiguration;
import org.junit.jupiter.api.BeforeAll;

import java.util.Map;

@GraylogBackendConfiguration
public class CompoundFieldsAggregationIT {

    private static GraylogApis api;

    @BeforeAll
    static void beforeAll(GraylogApis graylogApis) {
        api = graylogApis;
    }

    @FullBackendTest
    void aggregate() throws Exception {
        final PortBoundGelfInputApi gelf = api.gelf().createGelfHttpInput();
        try (final var env1 = api.createEnvironment(gelf)) {
            try (final var env2 = api.createEnvironment(gelf)) {
                env1.putDeflectorFieldMapping("my_ip", "ip");
                env2.putDeflectorFieldMapping("my_ip", "keyword");
                env1.ingestMessage(Map.of(
                        "short_message", "compound-field-test-a",
                        "_my_ip", "192.168.1.1"
                ));
                env2.ingestMessage(Map.of(
                        "short_message", "compound-field-test-a",
                        "_my_ip", "192.168.1.1"
                ));

                api.post("/search/aggregate", """
                        {
                        	"group_by": [
                        		{
                        			"field": "my_ip"
                        		}
                        	],
                        	"metrics": [
                        		{
                        			"function": "count",
                        			"field": "my_ip",
                        			"sort": "desc"
                        		}
                        	]
                        }
                        """, 200);
            }
        }

    }
}
