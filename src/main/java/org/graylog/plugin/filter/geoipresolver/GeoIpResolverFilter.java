package org.graylog.plugin.filter.geoipresolver;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.Lists;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.Location;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.filters.MessageFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.base.Strings.isNullOrEmpty;

public class GeoIpResolverFilter implements MessageFilter {
    private static final Logger LOG = LoggerFactory.getLogger(GeoIpResolverFilter.class);
    // TODO: Match also IPv6 addresses
    private static final Pattern IP_PATTERN = Pattern.compile("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})");

    private DatabaseReader databaseReader;
    private final boolean shouldRunBeforeExtractors;
    private boolean enabled;

    private final Timer resolveTime;

    @Inject
    public GeoIpResolverFilter(@Named("geoip_resolver_database") String geoIpDatabase,
                               @Named("geoip_resolver_run_before_extractors") boolean shouldRunBeforeExtractors,
                               @Named("geoip_resolver_enabled") boolean enabled,
                               MetricRegistry metricRegistry) {
        try {
            final File database = new File(geoIpDatabase);
            this.databaseReader = new DatabaseReader.Builder(database).build();
            this.enabled = enabled;
        } catch (IOException e) {
            LOG.error("Could not open GeoIP database " + geoIpDatabase, e);
            this.enabled = false;
        }

        this.shouldRunBeforeExtractors = shouldRunBeforeExtractors;

        this.resolveTime = metricRegistry.timer(name(GeoIpResolverFilter.class, "resolveTime"));
    }

    @Override
    public boolean filter(Message message) {
        if (!enabled) {
            return false;
        }

        for (Map.Entry<String, Object> field : message.getFields().entrySet()) {
            String key = field.getKey() + "_geolocation";
            final List coordinates = extractGeoLocationInformation(field.getValue());
            if (coordinates.size() == 2) {
                // We will store the coordinates as a "lat,long" string
                final String stringGeoPoint = coordinates.get(1) + "," + coordinates.get(0);
                message.addField(key, stringGeoPoint);
            }
        }

        return false;
    }

    protected String getIpFromFieldValue(String fieldValue) {
        Matcher matcher = IP_PATTERN.matcher(fieldValue);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    protected List<Double> extractGeoLocationInformation(Object fieldValue) {
        final List<Double> coordinates = Lists.newArrayList();

        if (!(fieldValue instanceof String) || isNullOrEmpty((String) fieldValue)) {
            return coordinates;
        }

        final String stringFieldValue = (String) fieldValue;
        final String ip = this.getIpFromFieldValue(stringFieldValue);
        if (isNullOrEmpty(ip)) {
            return coordinates;
        }

        try {
            try (Timer.Context ignored = resolveTime.time()) {
                final InetAddress ipAddress = InetAddress.getByName(ip);
                final CityResponse response = databaseReader.city(ipAddress);
                final Location location = response.getLocation();
                coordinates.add(location.getLongitude());
                coordinates.add(location.getLatitude());
            }
        } catch (Exception e) {
            LOG.debug("Could not get location from IP " + ip, e);
        }

        return coordinates;
    }

    @Override
    public String getName() {
        return "GeoIP resolver";
    }

    @Override
    public int getPriority() {
        // MAGIC NUMBER: 10 is the priority of the ExtractorFilter, we either run before or after it, depending on what the user wants.
        return 10 - (shouldRunBeforeExtractors ? 1 : -1);
    }
}
