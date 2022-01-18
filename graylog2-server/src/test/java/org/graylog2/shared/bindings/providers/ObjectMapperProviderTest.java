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

        final String forDefault = om.get().writeValueAsString(ImmutableMap.of("date", new DateTime(0)));
        final String forNull = om.getForTimeZone(null).writeValueAsString(ImmutableMap.of("date", new DateTime(0)));
        final String forUtc = om.getForTimeZone(DateTimeZone.forID("UTC"))
                .writeValueAsString(ImmutableMap.of("date", new DateTime(0)));
        final String forBerlin = om.getForTimeZone(DateTimeZone.forID("Europe/Berlin"))
                .writeValueAsString(ImmutableMap.of("date", new DateTime(0)));

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
