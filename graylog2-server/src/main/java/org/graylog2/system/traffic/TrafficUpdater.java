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
package org.graylog2.system.traffic;

import org.graylog2.plugin.system.NodeId;
import org.joda.time.DateTime;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public interface TrafficUpdater {
    void updateTraffic(DateTime observationTime,
                       NodeId nodeId,
                       long inLastMinute,
                       long outLastMinute,
                       long decodedLastMinute);

    static DateTime getDayBucketStart(DateTime observationTime) {
        return observationTime.withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);
    }

    static DateTime getHourBucketStart(DateTime observationTime) {
        return observationTime.withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);
    }

    static Map<DateTime, Long> aggregateToDaily(Map<DateTime, Long> histogram) {
        return histogram.entrySet().stream()
                .collect(Collectors.groupingBy(entry -> entry.getKey().withTimeAtStartOfDay(),
                        TreeMap::new,
                        Collectors.mapping(Map.Entry::getValue, Collectors.summingLong(Long::valueOf))));
    }
}
