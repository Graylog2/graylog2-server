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
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.net.InetAddresses;
import com.google.inject.assistedinject.Assisted;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.model.CountryResponse;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Location;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog.plugins.map.config.DatabaseType;
import org.graylog2.plugin.lookup.LookupCachePurge;
import org.graylog2.plugin.lookup.LookupDataAdapter;
import org.graylog2.plugin.lookup.LookupDataAdapterConfiguration;
import org.graylog2.plugin.lookup.LookupResult;
import org.graylog2.plugin.utilities.FileInfo;
import org.joda.time.Duration;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.slf4j.LoggerFactory.getLogger;

public class MaxmindDataAdapter extends LookupDataAdapter {

    private static final Logger LOG = getLogger(MaxmindDataAdapter.class);

    public static final String NAME = "maxmind_geoip";
    private final Config config;
    private final AtomicReference<DatabaseReader> databaseReader = new AtomicReference<>();
    private FileInfo fileInfo = FileInfo.empty();

    @Inject
    protected MaxmindDataAdapter(@Assisted("id") String id,
                                 @Assisted("name") String name,
                                 @Assisted LookupDataAdapterConfiguration config,
                                 MetricRegistry metricRegistry) {
        super(id, name, config, metricRegistry);
        this.config = (Config) config;
    }

    @Override
    protected void doStart() throws Exception {
        Path path = Paths.get(config.path());
        fileInfo = FileInfo.forPath(path);

        if (!Files.isReadable(path)) {
            LOG.warn("Cannot read database file {}", config.path());
            setError(new IllegalStateException("Cannot read database file " + config.path()));
        } else {
            try {
                this.databaseReader.set(loadReader(path.toFile()));
            } catch (Exception e) {
                LOG.warn("Unable to read data base file {}", config.path());
                setError(e);
            }
        }
    }

    @Override
    protected void doStop() throws Exception {
        final DatabaseReader databaseReader = this.databaseReader.get();
        if (databaseReader != null) {
            databaseReader.close();
        }
    }

    @Override
    public Duration refreshInterval() {
        if (config.checkIntervalUnit() == null || config.checkInterval() == 0) {
            return Duration.ZERO;
        }
        //noinspection ConstantConditions
        return Duration.millis(config.checkIntervalUnit().toMillis(config.checkInterval()));
    }

    @Override
    protected void doRefresh(LookupCachePurge cachePurge) throws Exception {
        try {
            final FileInfo.Change databaseFileCheck = fileInfo.checkForChange();
            if (!databaseFileCheck.isChanged() && !getError().isPresent()) {
                return;
            }

            // file has different attributes, let's reload it
            LOG.debug("MaxMind database file has changed, reloading it from {}", config.path());
            final DatabaseReader oldReader = this.databaseReader.get();
            try {
                this.databaseReader.set(loadReader(Paths.get(config.path()).toFile()));
                cachePurge.purgeAll();
                if (oldReader != null) {
                    oldReader.close();
                }
                fileInfo = databaseFileCheck.fileInfo();
                clearError();
            } catch (IOException e) {
                LOG.warn("Unable to load changed database file, leaving old one intact. Error message: {}", e.getMessage());
                setError(e);
            }
        } catch (IllegalArgumentException iae) {
            LOG.error("Unable to refresh MaxMind database file: {}", iae.getMessage());
            setError(iae);
        }
    }

    private DatabaseReader loadReader(File file) throws IOException {
        return new DatabaseReader.Builder(file).build();
    }

    @Override
    protected LookupResult doGet(Object key) {
        final InetAddress addr;
        if (key instanceof InetAddress) {
            addr = (InetAddress) key;
        } else {
            // try to convert it somehow
            try {
                addr = InetAddresses.forString(key.toString());
            } catch (IllegalArgumentException e) {
                LOG.warn("Unable to parse IP address, returning empty result.");
                return LookupResult.empty();
            }
        }
        final DatabaseReader reader = this.databaseReader.get();
        switch (config.dbType()) {
            case MAXMIND_CITY:
                try {
                    final CityResponse city = reader.city(addr);
                    if (city == null) {
                        LOG.debug("No city data for IP address {}, returning empty result.", addr);
                        return LookupResult.empty();
                    }

                    final Location location = city.getLocation();
                    final Map<Object, Object> map = new HashMap<>();
                    map.put("city", city.getCity());
                    map.put("continent", city.getContinent());
                    map.put("country", city.getCountry());
                    map.put("location", location);
                    map.put("postal", city.getPostal());
                    map.put("registered_country", city.getRegisteredCountry());
                    map.put("represented_country", city.getRepresentedCountry());
                    map.put("subdivisions", city.getSubdivisions());
                    map.put("traits", city.getTraits());

                    final String singleValue;
                    if (location == null || location.getLatitude() == null || location.getLongitude() == null) {
                        singleValue = null;
                    } else {
                        singleValue = location.getLatitude() + "," + location.getLongitude();
                        map.put("coordinates", singleValue);
                    }
                    return LookupResult.multi(singleValue, map);
                } catch (AddressNotFoundException nfe) {
                    LOG.debug("Unable to look up city data for IP address {}, returning empty result.", addr, nfe);
                    return LookupResult.empty();
                } catch (Exception e) {
                    LOG.warn("Unable to look up city data for IP address {}, returning empty result.", addr, e);
                    return LookupResult.empty();
                }
            case MAXMIND_COUNTRY:
                try {
                    final CountryResponse countryResponse = reader.country(addr);
                    if (countryResponse == null) {
                        LOG.debug("No country data for IP address {}, returning empty result.", addr);
                        return LookupResult.empty();
                    }

                    final Country country = countryResponse.getCountry();
                    final Map<Object, Object> map = new HashMap<>();
                    map.put("continent", countryResponse.getContinent());
                    map.put("country", country);
                    map.put("registered_country", countryResponse.getRegisteredCountry());
                    map.put("represented_country", countryResponse.getRepresentedCountry());
                    map.put("traits", countryResponse.getTraits());

                    final String singleValue = country == null ? null : country.getIsoCode();
                    return LookupResult.multi(singleValue, map);
                } catch (AddressNotFoundException nfe) {
                    LOG.debug("Unable to look up country data for IP address {}, returning empty result.", addr, nfe);
                    return LookupResult.empty();
                } catch (Exception e) {
                    LOG.warn("Unable to look up country data for IP address {}, returning empty result.", addr, e);
                    return LookupResult.empty();
                }
        }

        return LookupResult.empty();
    }

    @Override
    public void set(Object key, Object value) {
        throw new UnsupportedOperationException();
    }

    @VisibleForTesting
    void setDatabaseReader(DatabaseReader databaseReader) {
        this.databaseReader.set(databaseReader);
    }

    @VisibleForTesting
    DatabaseReader getDatabaseReader() {
        return databaseReader.get();
    }

    public interface Factory extends LookupDataAdapter.Factory<MaxmindDataAdapter> {
        @Override
        MaxmindDataAdapter create(@Assisted("id") String id,
                                  @Assisted("name") String name,
                                  LookupDataAdapterConfiguration configuration);

        @Override
        MaxmindDataAdapter.Descriptor getDescriptor();
    }

    public static class Descriptor extends LookupDataAdapter.Descriptor<MaxmindDataAdapter.Config> {
        public Descriptor() {
            super(NAME, MaxmindDataAdapter.Config.class);
        }

        @Override
        public MaxmindDataAdapter.Config defaultConfiguration() {
            return MaxmindDataAdapter.Config.builder()
                    .type(NAME)
                    .checkInterval(1)
                    .checkIntervalUnit(TimeUnit.MINUTES)
                    .path("/etc/graylog/server/GeoLite2-City.mmdb")
                    .dbType(DatabaseType.MAXMIND_CITY)
                    .build();
        }
    }

    @AutoValue
    @WithBeanGetter
    @JsonAutoDetect
    @JsonDeserialize(builder = AutoValue_MaxmindDataAdapter_Config.Builder.class)
    @JsonTypeName(NAME)
    public static abstract class Config implements LookupDataAdapterConfiguration {

        @Override
        @JsonProperty(TYPE_FIELD)
        public abstract String type();

        @JsonProperty("path")
        @NotEmpty
        public abstract String path();

        @JsonProperty("database_type")
        @NotNull
        public abstract DatabaseType dbType();

        @JsonProperty("check_interval")
        @Min(0)
        public abstract long checkInterval();

        @Nullable
        @JsonProperty("check_interval_unit")
        public abstract TimeUnit checkIntervalUnit();

        public static Config.Builder builder() {
            return new AutoValue_MaxmindDataAdapter_Config.Builder();
        }

        @Override
        public Optional<Multimap<String, String>> validate() {
            final ArrayListMultimap<String, String> errors = ArrayListMultimap.create();

            final Path path = Paths.get(path());
            if (!Files.exists(path)) {
                errors.put("path", "The file does not exist.");
            } else if (!Files.isReadable(path)) {
                errors.put("path", "The file cannot be read.");
            }

            return errors.isEmpty() ? Optional.empty() : Optional.of(errors);
        }

        @AutoValue.Builder
        public abstract static class Builder {
            @JsonProperty(TYPE_FIELD)
            public abstract Config.Builder type(String type);

            @JsonProperty("path")
            public abstract Config.Builder path(String path);

            @JsonProperty("database_type")
            public abstract Builder dbType(DatabaseType dbType);

            @JsonProperty("check_interval")
            public abstract Builder checkInterval(long checkInterval);

            @JsonProperty("check_interval_unit")
            public abstract Builder checkIntervalUnit(@Nullable TimeUnit checkIntervalUnit);


            public abstract Config build();
        }
    }
}
