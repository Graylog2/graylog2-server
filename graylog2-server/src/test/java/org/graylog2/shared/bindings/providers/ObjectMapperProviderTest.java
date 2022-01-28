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
package org.graylog2.shared.bindings.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ObjectMapperProviderTest {
    @Test
    void returnsMapperWithTimeZoneSet() throws Exception {
        final ObjectMapperProvider om = new ObjectMapperProvider();

        final String forDefault = om.get().
                writeValueAsString(ImmutableMap.of("date", new DateTime(0, DateTimeZone.UTC)));
        final String forNull = om.getForTimeZone(null).
                writeValueAsString(ImmutableMap.of("date", new DateTime(0, DateTimeZone.UTC)));
        final String forUtc = om.getForTimeZone(DateTimeZone.forID("UTC"))
                .writeValueAsString(ImmutableMap.of("date", new DateTime(0, DateTimeZone.UTC)));
        final String forBerlin = om.getForTimeZone(DateTimeZone.forID("Europe/Berlin"))
                .writeValueAsString(ImmutableMap.of("date", new DateTime(0, DateTimeZone.UTC)));

        assertThat(forDefault).isEqualTo("{\"date\":\"1970-01-01T00:00:00.000Z\"}");
        assertThat(forDefault).isEqualTo(forNull).isEqualTo(forUtc);
        assertThat(forBerlin).isEqualTo("{\"date\":\"1970-01-01T01:00:00.000+01:00\"}");
    }

    @Test
    void returnsMapperForEveryTimeZone() {
        final ObjectMapperProvider om = new ObjectMapperProvider();
        final Set<DateTimeZone> availableZones = DateTimeZone.getAvailableIDs().stream()
                .map(DateTimeZone::forID)
                .collect(Collectors.toSet());
        final Set<ObjectMapper> mappers = availableZones.stream()
                .map(om::getForTimeZone)
                .collect(Collectors.toSet());
        assertThat(mappers).hasSameSizeAs(availableZones);
    }


}
