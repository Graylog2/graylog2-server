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
import com.eaio.uuid.UUID;
import com.google.common.collect.Maps;
import com.google.common.net.InetAddresses;
import org.graylog.plugins.map.ConditionalRunner;
import org.graylog.plugins.map.ResourceExistsCondition;
import org.graylog.plugins.map.config.GeoIpResolverConfig;
import org.graylog2.plugin.Message;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URISyntaxException;
import java.util.Map;

import static com.codahale.metrics.MetricRegistry.name;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(ConditionalRunner.class)
@ResourceExistsCondition(GeoIpResolverEngineTest.GEO_LITE2_CITY_MMDB)
public class GeoIpResolverEngineTest {
    static final String GEO_LITE2_CITY_MMDB = "/GeoLite2-City.mmdb";

    private MetricRegistry metricRegistry;
    private GeoIpResolverConfig config;

    @Before
    public void setUp() throws URISyntaxException {
        config = GeoIpResolverConfig.defaultConfig().toBuilder().enabled(true).dbPath(this.getTestDatabasePath()).build();
        metricRegistry = new MetricRegistry();
    }

    @After
    public void tearDown() {
        metricRegistry.removeMatching(MetricFilter.ALL);
        metricRegistry = null;
    }

    private String getTestDatabasePath() throws URISyntaxException {
        return this.getClass().getResource(GEO_LITE2_CITY_MMDB).toURI().getPath();
    }

    @Test
    public void getIpFromFieldValue() {
        final GeoIpResolverEngine resolver = new GeoIpResolverEngine(config, metricRegistry);
        final String ip = "127.0.0.1";

        assertEquals(InetAddresses.forString(ip), resolver.getIpFromFieldValue(ip));
        assertNull(resolver.getIpFromFieldValue("Message from \"127.0.0.1\""));
        assertNull(resolver.getIpFromFieldValue("Test message with no IP"));
    }

    @Test
    public void trimFieldValueBeforeLookup() {
        final GeoIpResolverEngine resolver = new GeoIpResolverEngine(config, metricRegistry);
        final String ip = "   2001:4860:4860::8888\t\n";

        assertNotNull(resolver.getIpFromFieldValue(ip));
    }

    @Test
    public void extractGeoLocationInformation() {
        final GeoIpResolverEngine resolver = new GeoIpResolverEngine(config, metricRegistry);

        assertTrue("Should extract geo location information from public addresses", resolver.extractGeoLocationInformation("1.2.3.4").isPresent());
        assertFalse("Should not extract geo location information from private addresses", resolver.extractGeoLocationInformation("192.168.0.1").isPresent());
        assertFalse("Should not extract geo location information numeric fields", resolver.extractGeoLocationInformation(42).isPresent());
        assertTrue("Should extract geo location information IP address fields", resolver.extractGeoLocationInformation(InetAddresses.forString("1.2.3.4")).isPresent());
    }

    @Test
    public void disabledFilterTest() {
        final GeoIpResolverEngine resolver = new GeoIpResolverEngine(config.toBuilder().enabled(false).build(), metricRegistry);

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

    private void assertFieldNotResolved(Message message, String fieldName, String errorMessage) {
        assertNull(errorMessage + " coordinates in " + fieldName, message.getField(fieldName + "_geolocation"));
        assertNull(errorMessage + " country in " + fieldName, message.getField(fieldName + "_country_code"));
        assertNull(errorMessage + " city in " + fieldName, message.getField(fieldName + "_city_name"));
    }

    private void assertFieldResolved(Message message, String fieldName, String errorMessage) {
        assertNotNull(errorMessage + " coordinates in " + fieldName, message.getField(fieldName + "_geolocation"));
        assertNotNull(errorMessage + " country in " + fieldName, message.getField(fieldName + "_country_code"));
        assertNotNull(errorMessage + " city in " + fieldName, message.getField(fieldName + "_city_name"));
        assertTrue("Location coordinates for " + fieldName + " should include a comma", ((String) message.getField(fieldName + "_geolocation")).contains(","));
    }

    @Test
    public void filterResolvesIpGeoLocation() {
        final GeoIpResolverEngine resolver = new GeoIpResolverEngine(config, metricRegistry);

        final Map<String, Object> messageFields = Maps.newHashMap();
        messageFields.put("_id", (new UUID()).toString());
        messageFields.put("source", "192.168.0.1");
        messageFields.put("message", "Hello from 1.2.3.4");
        messageFields.put("extracted_ip", "1.2.3.4");
        messageFields.put(Message.FIELD_GL2_REMOTE_IP, "1.2.3.4");
        messageFields.put("ipv6", "2001:4860:4860::8888");

        final Message message = new Message(messageFields);
        final boolean filtered = resolver.filter(message);

        assertFalse("Message should not be filtered out", filtered);
        assertEquals("Should have looked up three IPs", 3, metricRegistry.timer(name(GeoIpResolverEngine.class, "resolveTime")).getCount());
        assertFieldNotResolved(message, "source", "Should not have resolved private IP");
        assertFieldNotResolved(message, "message", "Should have resolved public IP");
        assertFieldNotResolved(message, "gl2_remote_ip", "Should not have resolved text with an IP");
        assertFieldResolved(message, "extracted_ip", "Should have resolved public IP");
        assertFieldResolved(message, "ipv6", "Should have resolved public IPv6");
    }
}