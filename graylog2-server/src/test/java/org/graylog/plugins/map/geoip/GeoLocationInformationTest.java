package org.graylog.plugins.map.geoip;

import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Location;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class GeoLocationInformationTest {

    @Test
    void testCreateWithNullLocation() {

        Assertions.assertDoesNotThrow(() -> GeoLocationInformation.create(null, createCountry(), createCity(), "N/A"));

    }

    @Test
    void testCreateWithNullLongLat() {

        Assertions.assertDoesNotThrow(() -> GeoLocationInformation.create(new Location(), createCountry(), createCity(), "N/A"));

    }

    @Test
    void testCreateWithNullCountry() {

        Assertions.assertDoesNotThrow(() -> GeoLocationInformation.create(null, null, createCity(), "N/A"));

    }

    @Test
    void testCreateWithNullCity() {

        Assertions.assertDoesNotThrow(() -> GeoLocationInformation.create(null, createCountry(), null, "N/A"));

    }

    private static Country createCountry() {
        Map<String, String> names = new HashMap<>();
        names.put("en", "United States");

        return new Country(Collections.singletonList("en"), null, 1, false, "US", names);
    }

    private static City createCity() {
        return new City(Collections.singletonList("en"), null, null, new HashMap<>());
    }

}
