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

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.eaio.uuid.UUID;
import com.google.common.collect.Maps;
import com.google.common.net.InetAddresses;
import org.graylog.plugins.map.config.DatabaseVendorType;
import org.graylog.plugins.map.config.GeoIpResolverConfig;
import org.graylog2.plugin.Message;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class GeoIpResolverEngineTest {

    @Mock
    private GeoIpResolver<GeoLocationInformation> maxMindCityResolver;

    @Mock
    private GeoIpResolver<GeoAsnInformation> maxMindAsnResolver;

    @Mock
    private GeoIpResolver<GeoLocationInformation> ipInfoCityResolver;

    @Mock
    private GeoIpResolver<GeoAsnInformation> ipInfoAsnResolver;

    @Mock
    GeoIpVendorResolverService geoIpVendorResolverService;

    private final GeoLocationInformation maxMindLocationInfo = GeoLocationInformation.create(1, 2, "US", "USA", "Houston", "Texas", "America/Chicago");
    private final GeoLocationInformation ipInfoLocationInfo = GeoLocationInformation.create(1, 2, "US", "N/A", "Houston", "Texas", "America/Chicago");
    private final GeoAsnInformation maxMindAsnInfo = GeoAsnInformation.create("Scamcast", "", "1000");
    private final GeoAsnInformation ipInfoAsnInfo = GeoAsnInformation.create("Scamcast", "001", "1000");

    private MetricRegistry metricRegistry;
    private GeoIpResolverConfig config;

    private InetAddress reservedIp;
    private InetAddress publicIp;

    @Before
    public void setUp() throws Exception {

        reservedIp = InetAddress.getByName("127.0.0.1");
        publicIp = InetAddress.getByName("96.110.152.253");

        MockitoAnnotations.openMocks(this).close();

        when(maxMindAsnResolver.isEnabled()).thenReturn(true);
        when(maxMindCityResolver.getGeoIpData(publicIp))
                .thenReturn(Optional.of(maxMindLocationInfo));
        when(maxMindAsnResolver.getGeoIpData(publicIp))
                .thenReturn(Optional.of(maxMindAsnInfo));
        when(maxMindCityResolver.isEnabled()).thenReturn(true);

        when(geoIpVendorResolverService.createCityResolver(any(GeoIpResolverConfig.class), any(Timer.class)))
                .thenReturn(maxMindCityResolver);
        when(geoIpVendorResolverService.createAsnResolver(any(GeoIpResolverConfig.class), any(Timer.class)))
                .thenReturn(maxMindAsnResolver);


        config = GeoIpResolverConfig.defaultConfig().toBuilder()
                .enforceGraylogSchema(true)
                .enabled(true)
                .cityDbPath("")
                .build();

        metricRegistry = new MetricRegistry();
    }

    @After
    public void tearDown() {
        metricRegistry.removeMatching(MetricFilter.ALL);
        metricRegistry = null;
    }

    @Test
    public void testPrivateIpSchemaEnforceOn() {

        testPrivateSourceIpWithSchemaEnforceFlag(true, "source_reserved_ip");
    }

    @Test
    public void testPrivateIpSchemaEnforceOff() {

        testPrivateSourceIpWithSchemaEnforceFlag(false, "source_ip_reserved_ip");
    }

    private void testPrivateSourceIpWithSchemaEnforceFlag(boolean enforceSchema, String expectedFieldName) {
        GeoIpResolverConfig conf = config.toBuilder().enforceGraylogSchema(enforceSchema).build();
        final GeoIpResolverEngine engine = new GeoIpResolverEngine(geoIpVendorResolverService, conf, metricRegistry);

        Map<String, Object> fields = new HashMap<>();
        fields.put("_id", java.util.UUID.randomUUID().toString());
        fields.put("source_ip", reservedIp);

        Message message = new Message(fields);
        engine.filter(message);

        String fieldNotFoundError = String.format(Locale.ENGLISH, "Field '%s' expected", expectedFieldName);
        Assert.assertTrue(fieldNotFoundError, message.hasField(expectedFieldName));

        Boolean expectedValue = true;
        String fieldValueError = String.format("Expected value for '%s' is '%s'", expectedFieldName, expectedValue);
        Assert.assertEquals(fieldValueError, expectedValue, message.getField(expectedFieldName));
    }

    @Test
    public void testPublicIpSchemaEnforceOn() {

        // With schema enforcement on, we expect the '_ip' to be dropped from 'source_ip' Geo fields.
        String[] expectedFields = {"source_geo_name", "source_geo_region", "source_geo_city", "source_geo_timezone",
                "source_geo_country", "source_geo_country_iso", "source_as_organization", "source_geo_coordinates",
                "source_as_number"};

        testSourceIpGeoDataFieldsWithSchemaEnforcementFlag(true, expectedFields);
    }

    @Test
    public void testPublicIpSchemaEnforceOff() {

        // With schema enforcement off, we expect 'source_ip' to be the prefix for ALL Geo data fields.
        String[] expectedFields = {"source_ip_geo_name", "source_ip_geo_region", "source_ip_geo_city", "source_ip_geo_timezone",
                "source_ip_geo_country", "source_ip_geo_country_iso", "source_ip_as_organization", "source_ip_geo_coordinates",
                "source_ip_as_number"};

        testSourceIpGeoDataFieldsWithSchemaEnforcementFlag(false, expectedFields);
    }

    private void testSourceIpGeoDataFieldsWithSchemaEnforcementFlag(boolean enforceSchema, String[] expectedFields) {
        GeoIpResolverConfig conf = config.toBuilder().enforceGraylogSchema(enforceSchema).build();
        final GeoIpResolverEngine engine = new GeoIpResolverEngine(geoIpVendorResolverService, conf, metricRegistry);

        Map<String, Object> fields = new HashMap<>();
        fields.put("_id", java.util.UUID.randomUUID().toString());
        fields.put("source_ip", publicIp);

        Message message = new Message(fields);
        engine.filter(message);

        for (String field : expectedFields) {
            String error = String.format(Locale.ENGLISH, "Field '%s' was not found", field);
            Assert.assertTrue(error, message.hasField(field));
        }
    }

    @Test
    public void testGetIpAddressFieldsEnforceGraylogSchema() {

        GeoIpResolverConfig conf = config.toBuilder().enforceGraylogSchema(true).build();
        final GeoIpResolverEngine engine = new GeoIpResolverEngine(geoIpVendorResolverService, conf, metricRegistry);

        Map<String, Object> fields = new HashMap<>();
        fields.put("_id", java.util.UUID.randomUUID().toString());
        fields.put("source_ip", "127.0.0.1");
        fields.put("src_ip", "127.0.0.1");
        fields.put("destination_ip", "127.0.0.1");
        fields.put("dest_ip", "127.0.0.1");
        fields.put("gl2_test", "127.0.0.1");


        Message message = new Message(fields);
        List<String> ipFields = engine.getIpAddressFields(message);

        //with the Graylog Schema enforced, only the source_ip and destination_ip should be returned
        Assertions.assertEquals(2, ipFields.size());
        Assertions.assertTrue(ipFields.contains("source_ip"));
        Assertions.assertTrue(ipFields.contains("destination_ip"));

    }

    @Test
    public void testGetIpAddressFieldsEnforceGraylogSchemaFalse() {

        GeoIpResolverConfig conf = config.toBuilder().enforceGraylogSchema(false).build();
        final GeoIpResolverEngine engine = new GeoIpResolverEngine(geoIpVendorResolverService, conf, metricRegistry);

        Map<String, Object> fields = new HashMap<>();
        fields.put("_id", java.util.UUID.randomUUID().toString());
        fields.put("source_ip", "127.0.0.1");
        fields.put("src_ip", "127.0.0.1");
        fields.put("destination_ip", "127.0.0.1");
        fields.put("dest_ip", "127.0.0.1");
        fields.put("gl2_test", "127.0.0.1");


        Message message = new Message(fields);
        List<String> ipFields = engine.getIpAddressFields(message);

        //without enforcing the Graylog Schema, all but the gl2_* fields should be returned.
        Assertions.assertEquals(5, ipFields.size());

    }

    @Test
    public void testFilterMaxMind() {

        final GeoIpResolverEngine engine = new GeoIpResolverEngine(geoIpVendorResolverService, config, metricRegistry);

        Map<String, Object> fields = new HashMap<>();
        fields.put("_id", java.util.UUID.randomUUID().toString());
        fields.put("source_ip", publicIp.getHostAddress());

        Message message = new Message(fields);
        engine.filter(message);

        String expectedGeoName = maxMindLocationInfo.cityName() + ", " + maxMindLocationInfo.countryIsoCode();
        Assertions.assertEquals(expectedGeoName, message.getField("source_geo_name"));
        Assertions.assertEquals(maxMindLocationInfo.region(), message.getField("source_geo_region"));
        Assertions.assertEquals(maxMindLocationInfo.cityName(), message.getField("source_geo_city"));
        Assertions.assertEquals(maxMindLocationInfo.timeZone(), message.getField("source_geo_timezone"));
        Assertions.assertEquals(maxMindLocationInfo.countryName(), message.getField("source_geo_country"));
        Assertions.assertEquals(maxMindAsnInfo.organization(), message.getField("source_as_organization"));
        Assertions.assertEquals(maxMindAsnInfo.asn(), message.getField("source_as_number"));

    }

    @Test
    public void testFilterIpInfo() {

        when(ipInfoAsnResolver.isEnabled()).thenReturn(true);
        when(ipInfoAsnResolver.getGeoIpData(publicIp)).thenReturn(Optional.of(ipInfoAsnInfo));
        when(ipInfoCityResolver.isEnabled()).thenReturn(true);
        when(ipInfoCityResolver.getGeoIpData(publicIp)).thenReturn(Optional.of(ipInfoLocationInfo));

        when(geoIpVendorResolverService.createCityResolver(any(GeoIpResolverConfig.class), any(Timer.class)))
                .thenReturn(ipInfoCityResolver);
        when(geoIpVendorResolverService.createAsnResolver(any(GeoIpResolverConfig.class), any(Timer.class)))
                .thenReturn(ipInfoAsnResolver);

        GeoIpResolverConfig conf = config.toBuilder()
                .databaseVendorType(DatabaseVendorType.IPINFO)
                .build();

        final GeoIpResolverEngine engine = new GeoIpResolverEngine(geoIpVendorResolverService, conf, metricRegistry);

        Map<String, Object> fields = new HashMap<>();
        fields.put("_id", java.util.UUID.randomUUID().toString());
        fields.put("source_ip", publicIp.getHostAddress());

        Message message = new Message(fields);
        engine.filter(message);

        String expectedGeoName = ipInfoLocationInfo.cityName() + ", " + ipInfoLocationInfo.countryIsoCode();
        Assertions.assertEquals(expectedGeoName, message.getField("source_geo_name"));
        Assertions.assertEquals(ipInfoLocationInfo.region(), message.getField("source_geo_region"));
        Assertions.assertEquals(ipInfoLocationInfo.cityName(), message.getField("source_geo_city"));
        Assertions.assertEquals(ipInfoLocationInfo.timeZone(), message.getField("source_geo_timezone"));
        Assertions.assertFalse(message.hasField("source_geo_country"));
        Assertions.assertEquals(ipInfoLocationInfo.countryIsoCode(), message.getField("source_geo_country_iso"));
        Assertions.assertEquals(ipInfoAsnInfo.organization(), message.getField("source_as_organization"));
        Assertions.assertEquals(ipInfoAsnInfo.asn(), message.getField("source_as_number"));
    }

    @Test
    public void testFilterWithReservedIpAddress() {

        final GeoIpResolverEngine engine = new GeoIpResolverEngine(geoIpVendorResolverService, config, metricRegistry);

        Map<String, Object> fields = new HashMap<>();
        fields.put("_id", java.util.UUID.randomUUID().toString());
        fields.put("source_ip", "127.0.0.1");

        Message message = new Message(fields);
        engine.filter(message);
        Assertions.assertTrue(message.hasField("source_reserved_ip"));

    }

    @Test
    public void getIpFromFieldValue() {

        when(geoIpVendorResolverService.createCityResolver(any(GeoIpResolverConfig.class), any(Timer.class)))
                .thenReturn(maxMindCityResolver);

        when(geoIpVendorResolverService.createAsnResolver(any(GeoIpResolverConfig.class), any(Timer.class)))
                .thenReturn(maxMindAsnResolver);

        final GeoIpResolverEngine engine = new GeoIpResolverEngine(geoIpVendorResolverService, config, metricRegistry);
        final String ip = "127.0.0.1";

        assertEquals(InetAddresses.forString(ip), engine.getIpFromFieldValue(ip));
        assertNull(engine.getIpFromFieldValue("Message from \"127.0.0.1\""));
        assertNull(engine.getIpFromFieldValue("Test message with no IP"));
    }

    @Test
    public void trimFieldValueBeforeLookup() {
        final GeoIpResolverEngine resolver = new GeoIpResolverEngine(geoIpVendorResolverService, config, metricRegistry);
        final String ip = "   2001:4860:4860::8888\t\n";

        assertNotNull(resolver.getIpFromFieldValue(ip));
    }

    @Test
    public void disabledFilterTest() {

        when(maxMindCityResolver.isEnabled()).thenReturn(false);
        when(maxMindAsnResolver.isEnabled()).thenReturn(false);

        final GeoIpResolverEngine resolver = new GeoIpResolverEngine(geoIpVendorResolverService, config.toBuilder().enabled(false).build(), metricRegistry);

        final Map<String, Object> messageFields = Maps.newHashMap();
        messageFields.put("_id", (new UUID()).toString());
        messageFields.put("source", "192.168.0.1");
        messageFields.put("message", "Hello from 1.2.3.4");
        messageFields.put("extracted_ip", "1.2.3.4");
        messageFields.put("ipv6", "2001:4860:4860::8888");

        final Message message = new Message(messageFields);
        final boolean filtered = resolver.filter(message);

        assertFalse("Message should not be filtered out", filtered);
        assertEquals("Filter should not add new message fields", messageFields.size(), message.getFields().size());
    }
}
