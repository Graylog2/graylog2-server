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
package org.graylog.metrics.prometheus;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Metric;
import com.google.common.collect.ImmutableList;
import io.prometheus.client.dropwizard.samplebuilder.MapperConfig;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PrometheusMetricFilterTest {
    @Test
    void test() {
        final List<MapperConfig> mapperConfigs = ImmutableList.of(
                new MapperConfig("foo.*.bar.*.baz", "test1", Collections.emptyMap()),
                new MapperConfig("hello.world", "test2", Collections.emptyMap())
        );

        final PrometheusMetricFilter filter = new PrometheusMetricFilter(mapperConfigs);
        final Metric metric = new Counter();

        assertThat(filter.matches("foo.123.bar.456.baz", metric)).isTrue();
        assertThat(filter.matches("foo.abc.bar.456.baz", metric)).isTrue();
        assertThat(filter.matches("foo.123.nope.bar.456.baz", metric)).isFalse();
        assertThat(filter.matches("nope.foo.123.bar.456.baz", metric)).isFalse();
        assertThat(filter.matches("foo.123.bar.456.baz.nope", metric)).isFalse();
        assertThat(filter.matches("hello.world", metric)).isTrue();
        assertThat(filter.matches("hello.123.world", metric)).isFalse();
    }
}
