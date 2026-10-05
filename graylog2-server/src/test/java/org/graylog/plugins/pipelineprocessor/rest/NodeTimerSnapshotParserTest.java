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
package org.graylog.plugins.pipelineprocessor.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.rest.models.system.metrics.responses.MetricsSummaryResponse;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NodeTimerSnapshotParserTest {

    private final NodeTimerSnapshotParser parser = new NodeTimerSnapshotParser(new ObjectMapper());

    @Test
    void parsesEmptyResponse() {
        final NodeTimerSnapshot snapshot = parser.parse(MetricsSummaryResponse.create(List.of()));
        assertThat(snapshot.isEmpty()).isTrue();
    }

    @Test
    void parsesTimerEntry() {
        final NodeTimerSnapshot snapshot = parser.parse(responseWith(
                timerEntry("metric.foo", 123.0d, 45.0d)
        ));
        assertThat(snapshot.cost("metric.foo")).isEqualTo(123.0d * 45.0d);
    }

    @Test
    void missingFieldsBecomeZero() {
        final Map<String, Object> entry = timerEntry("metric.foo", 100.0d, 10.0d);
        @SuppressWarnings("unchecked")
        final Map<String, Object> rate = (Map<String, Object>) ((Map<String, Object>) entry.get("metric")).get("rate");
        rate.remove("fifteen_minute");

        final NodeTimerSnapshot snapshot = parser.parse(responseWith(entry));
        assertThat(snapshot.cost("metric.foo")).isEqualTo(0.0d);
    }

    @Test
    void skipsNonStringName() {
        final Map<String, Object> entry = timerEntry("metric.foo", 100.0d, 10.0d);
        entry.put("full_name", 42);

        assertThat(parser.parse(responseWith(entry)).isEmpty()).isTrue();
    }

    @Test
    void skipsNonMapMetric() {
        assertThat(parser.parse(responseWith(Map.of(
                "full_name", "metric.foo",
                "metric", "not-a-map"
        ))).isEmpty()).isTrue();
    }

    @Test
    void skipsEntryWithoutRateOrTime() {
        final NodeTimerSnapshot snapshot = parser.parse(responseWith(Map.of(
                "full_name", "metric.foo",
                "metric", Map.of("rate_unit", "events/second")
        )));
        assertThat(snapshot.isEmpty()).isTrue();
    }

    @Test
    void parsesValidEntriesAndSkipsBad() {
        final NodeTimerSnapshot snapshot = parser.parse(responseWith(
                timerEntry("metric.good", 10.0d, 5.0d),
                Map.of("full_name", 0, "metric", Map.of()),
                timerEntry("metric.also-good", 2.0d, 3.0d)
        ));
        assertThat(snapshot.cost("metric.good")).isEqualTo(50.0d);
        assertThat(snapshot.cost("metric.also-good")).isEqualTo(6.0d);
    }

    @SafeVarargs
    private static MetricsSummaryResponse responseWith(Map<String, Object>... entries) {
        return MetricsSummaryResponse.create(List.of(entries));
    }

    private static Map<String, Object> timerEntry(String fullName, double fifteenMinuteRate, double meanMicros) {
        final Map<String, Object> rate = new HashMap<>();
        rate.put("fifteen_minute", fifteenMinuteRate);
        final Map<String, Object> time = new HashMap<>();
        time.put("mean", meanMicros);
        final Map<String, Object> metric = new HashMap<>();
        metric.put("rate", rate);
        metric.put("time", time);
        final Map<String, Object> entry = new HashMap<>();
        entry.put("full_name", fullName);
        entry.put("name", fullName);
        entry.put("type", "timer");
        entry.put("metric", metric);
        return entry;
    }
}
