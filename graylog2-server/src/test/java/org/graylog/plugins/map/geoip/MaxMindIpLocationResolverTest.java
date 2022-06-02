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

import com.codahale.metrics.Timer;
import com.codahale.metrics.UniformReservoir;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Location;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MaxMindIpLocationResolverTest {

    private AutoCloseable mocks;

    @Mock
    private MaxMindIpLocationResolver resolver;

    @BeforeAll
    void init() {
        mocks = MockitoAnnotations.openMocks(this);
        Timer timer = new Timer(new UniformReservoir(1));
        when(resolver.getTimer()).thenReturn(timer.time());
        when(resolver.doGetGeoIpData(any(InetAddress.class))).thenCallRealMethod();

    }

    @AfterAll
    void cleanUp() throws Exception {
        mocks.close();
    }


    @Test
    void testDoGetGeoIpData() throws IOException, GeoIp2Exception {

        Country country = createCountry();
        City city = createCity();
        Location location = createLocation();
        CityResponse cityResponse = createCityResponse(country, city, location);

        when(resolver.getCityResponse(any(InetAddress.class))).thenReturn(cityResponse);

        InetAddress address = InetAddress.getByName("localhost");
        Optional<GeoLocationInformation> optInfo = resolver.doGetGeoIpData(address);

        assertTrue(optInfo.isPresent());
        GeoLocationInformation info = optInfo.get();
        assertEquals(country.getName(), info.countryName());
        assertEquals(country.getIsoCode(), info.countryIsoCode());
        assertEquals(city.getName(), info.cityName());
        assertEquals(location.getLatitude(), info.latitude());
        assertEquals(location.getLongitude(), info.longitude());
        assertEquals(location.getTimeZone(), info.timeZone());

    }

    @Test
    void testCityResponseWithNullCountry() throws IOException, GeoIp2Exception {

        CityResponse cityResponse = createCityResponse(null, createCity(), createLocation());

        when(resolver.getCityResponse(any(InetAddress.class))).thenReturn(cityResponse);

        InetAddress address = InetAddress.getByName("localhost");
        Optional<GeoLocationInformation> optInfo = resolver.doGetGeoIpData(address);

        assertTrue(optInfo.isPresent());
        GeoLocationInformation info = optInfo.get();
        assertEquals("N/A", info.countryName());
        assertEquals("N/A", info.countryIsoCode());

    }

    @Test
    void testCityResponseWithNullCity() throws IOException, GeoIp2Exception {

        CityResponse cityResponse = createCityResponse(createCountry(), null, createLocation());

        when(resolver.getCityResponse(any(InetAddress.class))).thenReturn(cityResponse);

        InetAddress address = InetAddress.getByName("localhost");
        Optional<GeoLocationInformation> optInfo = resolver.doGetGeoIpData(address);
        assertTrue(optInfo.isPresent());
        GeoLocationInformation info = optInfo.get();
        assertEquals("N/A", info.cityName());

    }

    @Test
    void testCityResponseWithNullLocation() throws IOException, GeoIp2Exception {

        CityResponse cityResponse = createCityResponse(createCountry(), createCity(), null);

        when(resolver.getCityResponse(any(InetAddress.class))).thenReturn(cityResponse);

        InetAddress address = InetAddress.getByName("localhost");
        Optional<GeoLocationInformation> optInfo = resolver.doGetGeoIpData(address);
        assertNonNullLocation(optInfo);

    }

    @Test
    void testCityResponseWithNullLongLat() throws IOException, GeoIp2Exception {

        Location location = new Location(null, null, null, null, null, null, "America/Chicago");
        CityResponse cityResponse = createCityResponse(createCountry(), createCity(), location);

        when(resolver.getCityResponse(any(InetAddress.class))).thenReturn(cityResponse);

        InetAddress address = InetAddress.getByName("localhost");
        Optional<GeoLocationInformation> optInfo = resolver.doGetGeoIpData(address);
        assertNonNullLocation(optInfo);

    }

    private void assertNonNullLocation(Optional<GeoLocationInformation> optInfo) {
        assertTrue(optInfo.isPresent());
        GeoLocationInformation info = optInfo.get();

        assertEquals(0.0, info.latitude());
        assertEquals(0.0, info.longitude());
    }

    private CityResponse createCityResponse(Country country, City city, Location location) {

        return new CityResponse(city, null, country, location, null, null,
                country, null, null, null);
    }

    private static Country createCountry() {
        Map<String, String> names = new HashMap<>();
        names.put("en", "Mali");

        return new Country(Collections.singletonList("en"), null, 1, false, "US", names);
    }

    private static City createCity() {
        final HashMap<String, String> names = new HashMap<>();
        names.put("en", "Timbuktu");
        return new City(Collections.singletonList("en"), 1, 1, names);
    }

    private static Location createLocation() {
        return new Location(10, 10_000_000, 16.766945605833932, -3.003387490676568, 1, 1, "GMT");
    }
}
