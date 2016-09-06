/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.map.geoip;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.eaio.uuid.UUID;
import com.google.common.collect.Maps;
import com.google.common.net.InetAddresses;
import org.graylog.plugins.map.config.GeoIpResolverConfig;
import org.graylog2.plugin.Message;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.codahale.metrics.MetricRegistry.name;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class GeoIpResolverEngineTest {

    private MetricRegistry metricRegistry;
    private GeoIpResolverConfig config;

    @BeforeMethod
    public void setUp() {
        config = GeoIpResolverConfig.defaultConfig().toBuilder().enabled(true).dbPath(this.getTestDatabasePath()).build();
        metricRegistry = new MetricRegistry();
    }

    @AfterMethod
    public void tearDown() {
        metricRegistry.removeMatching(MetricFilter.ALL);
        metricRegistry = null;
    }

    private String getTestDatabasePath() {
        String path = "";

        try {
            path = this.getClass().getResource("/GeoLite2-City.mmdb").toURI().getPath();
        } catch (URISyntaxException e) {
            System.err.println("Could not get test geo location database: " + e);
        }

        return path;
    }

    @Test
    public void getIpFromFieldValue() throws Exception {
        final GeoIpResolverEngine resolver = new GeoIpResolverEngine(config, metricRegistry);
        final String ip = "127.0.0.1";

        assertEquals(resolver.getIpFromFieldValue(ip), InetAddresses.forString(ip));
        assertNull(resolver.getIpFromFieldValue("Message from \"127.0.0.1\""));
        assertNull(resolver.getIpFromFieldValue("Test message with no IP"));
    }

    @Test
    public void trimFieldValueBeforeLookup() throws Exception {
        final GeoIpResolverEngine resolver = new GeoIpResolverEngine(config, metricRegistry);
        final String ip = "   2001:4860:4860::8888\t\n";

        assertNotNull(resolver.getIpFromFieldValue(ip));
    }

    @Test
    public void extractGeoLocationInformation() throws Exception {
        final GeoIpResolverEngine resolver = new GeoIpResolverEngine(config, metricRegistry);

        assertTrue(resolver.extractGeoLocationInformation("1.2.3.4").isPresent(), "Should extract geo location information from public addresses");
        assertFalse(resolver.extractGeoLocationInformation("192.168.0.1").isPresent(), "Should not extract geo location information from private addresses");
        assertFalse(resolver.extractGeoLocationInformation(42).isPresent(), "Should not extract geo location information numeric fields");
        assertTrue(resolver.extractGeoLocationInformation(InetAddresses.forString("1.2.3.4")).isPresent(), "Should extract geo location information IP address fields");
    }

    @Test
    public void disabledFilterTest() throws Exception {
        final GeoIpResolverEngine resolver = new GeoIpResolverEngine(config.toBuilder().enabled(false).build(), metricRegistry);

        final Map<String, Object> messageFields = Maps.newHashMap();
        messageFields.put("_id", (new UUID()).toString());
        messageFields.put("source", "192.168.0.1");
        messageFields.put("message", "Hello from 1.2.3.4");
        messageFields.put("extracted_ip", "1.2.3.4");
        messageFields.put("ipv6", "2001:4860:4860::8888");

        final Message message = new Message(messageFields);
        final boolean filtered = resolver.filter(message);

        assertFalse(filtered, "Message should not be filtered out");
        assertEquals(message.getFields().size(), messageFields.size(), "Filter should not add new message fields");
    }

    @Test
    public void filterResolvesIpGeoLocation() throws Exception {
        final GeoIpResolverEngine resolver = new GeoIpResolverEngine(config, metricRegistry);

        final Map<String, Object> messageFields = Maps.newHashMap();
        messageFields.put("_id", (new UUID()).toString());
        messageFields.put("source", "192.168.0.1");
        messageFields.put("message", "Hello from 1.2.3.4");
        messageFields.put("extracted_ip", "1.2.3.4");
        messageFields.put("ipv6", "2001:4860:4860::8888");

        final Message message = new Message(messageFields);
        final boolean filtered = resolver.filter(message);

        assertFalse(filtered, "Message should not be filtered out");
        assertEquals(message.getFields().size(), messageFields.size() + 2, "Filter should add new message fields");
        assertEquals(metricRegistry.timer(name(GeoIpResolverEngine.class, "resolveTime")).getCount(), 3, "Should have looked up three IPs");
        assertNull(message.getField("source_geolocation"), "Should not have resolved private IP");
        assertNull(message.getField("message_geolocation"), "Should have resolved public IP inside message");
        assertNotNull(message.getField("extracted_ip_geolocation"), "Should have resolved public IP inside extracted_ip");
        assertNotNull(message.getField("ipv6_geolocation"), "Should have resolved public IPv6 inside ipv6");
    }
}