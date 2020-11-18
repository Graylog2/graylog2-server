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

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.net.InetAddresses;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Country;
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
            if (!key.startsWith(Message.INTERNAL_FIELD_PREFIX)) {
                final Optional<GeoLocationInformation> geoLocationInformation = extractGeoLocationInformation(field.getValue());
                geoLocationInformation.ifPresent(locationInformation -> {
                    // We will store the coordinates as a "lat,long" string
                    message.addField(key + "_geolocation", locationInformation.latitude() + "," + locationInformation.longitude());
                    message.addField(key + "_country_code", locationInformation.countryIsoCode());
                    message.addField(key + "_city_name", locationInformation.cityName());
                });
            }
        }

        return false;
    }

    @VisibleForTesting
    Optional<GeoLocationInformation> extractGeoLocationInformation(Object fieldValue) {
        final InetAddress ipAddress;
        if (fieldValue instanceof InetAddress) {
            ipAddress = (InetAddress) fieldValue;
        } else if (fieldValue instanceof String) {
            ipAddress = getIpFromFieldValue((String) fieldValue);
        } else {
            ipAddress = null;
        }

        GeoLocationInformation geoLocationInformation = null;
        if (ipAddress != null) {
            try (Timer.Context ignored = resolveTime.time()) {
                final CityResponse response = databaseReader.city(ipAddress);
                final Location location = response.getLocation();
                final Country country = response.getCountry();
                final City city = response.getCity();

                geoLocationInformation = GeoLocationInformation.create(
                        location.getLatitude(), location.getLongitude(),
                        country.getGeoNameId() != null ? country.getIsoCode() : "N/A",
                        city.getGeoNameId() != null ? city.getName() : "N/A" // calling to .getName() may throw a NPE
                );
            } catch (Exception e) {
                LOG.debug("Could not get location from IP {}", ipAddress.getHostAddress(), e);
            }
        }

        return Optional.ofNullable(geoLocationInformation);
    }

    @Nullable
    @VisibleForTesting
    InetAddress getIpFromFieldValue(String fieldValue) {
        try {
            return InetAddresses.forString(fieldValue.trim());
        } catch (IllegalArgumentException e) {
            // Do nothing, field is not an IP
        }

        return null;
    }

    @AutoValue
    static abstract class GeoLocationInformation {
        public abstract double latitude();

        public abstract double longitude();

        public abstract String countryIsoCode();

        public abstract String cityName();

        public static GeoLocationInformation create(double latitude, double longitude, String countryIsoCode, String cityName) {
            return new AutoValue_GeoIpResolverEngine_GeoLocationInformation(latitude, longitude, countryIsoCode, cityName);
        }
    }
}
