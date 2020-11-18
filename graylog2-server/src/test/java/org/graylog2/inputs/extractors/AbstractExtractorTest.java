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
package org.graylog2.inputs.extractors;

import com.codahale.metrics.MetricRegistry;
import org.graylog2.plugin.inputs.Converter;
import org.junit.Before;
import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class AbstractExtractorTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    protected MetricRegistry metricRegistry;

    @Before
    public void setUp() throws Exception {
        metricRegistry = new MetricRegistry();
    }

    static List<Converter> noConverters() {
        return Collections.emptyList();
    }

    static Map<String, Object> noConfig() {
        return Collections.emptyMap();
    }
}
