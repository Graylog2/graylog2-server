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

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.auto.value.AutoValue;
import com.google.common.net.InetAddresses;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.Location;
import org.graylog.plugins.map.config.GeoIpResolverConfig;
import org.graylog2.plugin.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;

import static com.codahale.metrics.MetricRegistry.name;

public class GeoIpResolverEngine {
    private static final Logger LOG = LoggerFactory.getLogger(GeoIpResolverEngine.class);
    private static final String INTERNAL_FIELD_PREFIX = "gl2_";

    private final Timer resolveTime;
    private DatabaseReader databaseReader;
    private boolean enabled;


    public GeoIpResolverEngine(GeoIpResolverConfig config, MetricRegistry metricRegistry) {
        this.resolveTime = metricRegistry.timer(name(GeoIpResolverEngine.class, "resolveTime"));

        try {
            final File database = new File(config.dbPath());
            if (Files.exists(database.toPath())) {
                this.databaseReader = new DatabaseReader.Builder(database).build();
                this.enabled = config.enabled();
            } else {
                LOG.warn("GeoIP database file does not exist: {}", config.dbPath());
                this.enabled = false;
            }
        } catch (IOException e) {
            LOG.error("Could not open GeoIP database {}", config.dbPath(), e);
            this.enabled = false;
        }
    }

    public boolean filter(Message message) {
        if (!enabled) {
            return false;
        }

        for (Map.Entry<String, Object> field : message.getFields().entrySet()) {
            final String key = field.getKey();
            if (!key.startsWith(INTERNAL_FIELD_PREFIX)) {
                final Optional<Coordinates> coordinates = extractGeoLocationInformation(field.getValue());
                // We will store the coordinates as a "lat,long" string
                coordinates.ifPresent(c -> message.addField(key + "_geolocation", c.latitude() + "," + c.longitude()));
            }
        }

        return false;
    }

    protected Optional<Coordinates> extractGeoLocationInformation(Object fieldValue) {
        final InetAddress ipAddress;
        if (fieldValue instanceof InetAddress) {
            ipAddress = (InetAddress) fieldValue;
        } else if (fieldValue instanceof String) {
            ipAddress = getIpFromFieldValue((String) fieldValue);
        } else {
            ipAddress = null;
        }

        Coordinates coordinates = null;
        if (ipAddress != null) {
            try (Timer.Context ignored = resolveTime.time()) {
                final CityResponse response = databaseReader.city(ipAddress);
                final Location location = response.getLocation();
                coordinates = Coordinates.create(location.getLatitude(), location.getLongitude());
            } catch (Exception e) {
                LOG.debug("Could not get location from IP {}", ipAddress.getHostAddress(), e);
            }
        }

        return Optional.ofNullable(coordinates);
    }

    @Nullable
    protected InetAddress getIpFromFieldValue(String fieldValue) {
        try {
            return InetAddresses.forString(fieldValue.trim());
        } catch (IllegalArgumentException e) {
            // Do nothing, field is not an IP
        }

        return null;
    }

    @AutoValue
    static abstract class Coordinates {
        public abstract double latitude();

        public abstract double longitude();

        public static Coordinates create(double latitude, double longitude) {
            return new AutoValue_GeoIpResolverEngine_Coordinates(latitude, longitude);
        }
    }
}
