package org.graylog.plugin.filter.geoipresolver;

import com.beust.jcommander.internal.Maps;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.eaio.uuid.UUID;
import org.graylog2.plugin.Message;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import static com.codahale.metrics.MetricRegistry.name;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

public class GeoIpResolverFilterTest {

    private MetricRegistry metricRegistry;

    @BeforeMethod
    public void setUp() {
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
        final GeoIpResolverFilter resolver = new GeoIpResolverFilter(
                this.getTestDatabasePath(),
                false,
                true,
                metricRegistry);

        final String ip = "127.0.0.1";

        assertEquals(resolver.getIpFromFieldValue(ip), ip);
        assertEquals(resolver.getIpFromFieldValue("Message from \"127.0.0.1\""), ip);
        assertNull(resolver.getIpFromFieldValue("Test message with no IP"));
    }

    @Test
    public void extractGeoLocationInformation() throws Exception {
        final GeoIpResolverFilter resolver = new GeoIpResolverFilter(
                this.getTestDatabasePath(),
                false,
                true,
                metricRegistry);

        List<Double> coordinates = resolver.extractGeoLocationInformation("1.2.3.4");
        assertEquals(coordinates.size(), 2, "Should extract geo location information from public addresses");
        List<Double> coordinates2 = resolver.extractGeoLocationInformation("192.168.0.1");
        assertEquals(coordinates2.size(), 0, "Should not extract geo location information from private addresses");
    }

    @Test
    public void disabledFilterTest() throws Exception {
        final GeoIpResolverFilter resolver = new GeoIpResolverFilter(
                this.getTestDatabasePath(),
                false,
                false,
                metricRegistry);

        final Map<String, Object> messageFields = Maps.newHashMap();
        messageFields.put("_id", (new UUID()).toString());
        messageFields.put("source", "192.168.0.1");
        messageFields.put("message", "Hello from 1.2.3.4");
        messageFields.put("extracted_ip", "1.2.3.4");

        final Message message = new Message(messageFields);
        final boolean filtered = resolver.filter(message);

        assertFalse(filtered, "Message should not be filtered out");
        assertEquals(message.getFields().size(), messageFields.size(), "Filter should not add new message fields");
    }

    @Test
    public void filterResolvesIpGeoLocation() throws Exception {
        final GeoIpResolverFilter resolver = new GeoIpResolverFilter(
                this.getTestDatabasePath(),
                false,
                true,
                metricRegistry);

        final Map<String, Object> messageFields = Maps.newHashMap();
        messageFields.put("_id", (new UUID()).toString());
        messageFields.put("source", "192.168.0.1");
        messageFields.put("message", "Hello from 1.2.3.4");
        messageFields.put("extracted_ip", "1.2.3.4");

        final Message message = new Message(messageFields);
        final boolean filtered = resolver.filter(message);

        assertFalse(filtered, "Message should not be filtered out");
        assertEquals(message.getFields().size(), messageFields.size() + 2, "Filter should add new message fields");
        assertEquals(metricRegistry.timer(name(GeoIpResolverFilter.class, "resolveTime")).getCount(), 3, "Should have looked up three IPs");
        assertNull(message.getField("source_geolocation"), "Should not have resolved private IP");
        assertNotNull(message.getField("message_geolocation"), "Should have resolved public IP inside message");
        assertNotNull(message.getField("extracted_ip_geolocation"), "Should have resolved public IP inside extracted_ip");
    }
}