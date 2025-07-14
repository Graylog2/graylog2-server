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
package org.graylog.plugins.map.geoip;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeoLocationInformationTest {

    @Test
    public void testToIndexableWithAllValues() {
        GeoLocationInformation info = GeoLocationInformation.Builder.create()
                .latitude(10.0)
                .longitude(20.0)
                .countryIsoCode("US")
                .countryName("United States")
                .cityName("New York")
                .region("NY")
                .timeZone("America/New_York")
                .build();
        Map<String, Object> indexable = info.toIndexable();
        assertEquals(10.0, indexable.get("geo_latitude"));
        assertEquals(20.0, indexable.get("geo_longitude"));
        assertEquals("US", indexable.get("geo_country_iso_code"));
        assertEquals("United States", indexable.get("geo_country_name"));
        assertEquals("New York", indexable.get("geo_city_name"));
        assertEquals("NY", indexable.get("geo_region"));
        assertEquals("America/New_York", indexable.get("geo_time_zone"));
        assertEquals(7, indexable.size());
    }

    @Test
    public void testToIndexableWithNullValues() {
        Map<String, Object> indexableNulls = GeoLocationInformation.Builder.create().build().toIndexable();
        assertEquals(0, indexableNulls.size());
    }
}
