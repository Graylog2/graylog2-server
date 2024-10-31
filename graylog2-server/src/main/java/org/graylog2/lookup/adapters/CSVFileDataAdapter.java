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
package org.graylog2.lookup.adapters;

import au.com.bytecode.opencsv.CSVReader;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.net.InetAddresses;
import com.google.common.primitives.Ints;
import com.google.inject.assistedinject.Assisted;
import org.apache.commons.lang3.StringUtils;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.lookup.AllowedAuxiliaryPathChecker;
import org.graylog2.plugin.lookup.LookupCachePurge;
import org.graylog2.plugin.lookup.LookupDataAdapter;
import org.graylog2.plugin.lookup.LookupDataAdapterConfiguration;
import org.graylog2.plugin.lookup.LookupResult;
import org.graylog2.plugin.utilities.FileInfo;
import org.graylog2.utilities.IpSubnet;
import org.graylog2.utilities.ReservedIpChecker;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.graylog2.shared.utilities.StringUtils.f;

public class CSVFileDataAdapter extends LookupDataAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(CSVFileDataAdapter.class);

    public static final String NAME = "csvfile";

    /**
     * If the AllowedAuxiliaryPathChecker is enabled (one or more paths provided to the allowed_auxiliary_paths server
     * configuration property), then this error path will also be triggered for cases where the file does not exist.
     * This is unavoidable, since the AllowedAuxiliaryPathChecker tries to resolve symbolic links and relative paths,
     * which cannot be done if the file does not exist. Therefore this error message also indicates the possibility
     * that the file does not exist.
     */
    public static final String ALLOWED_PATH_ERROR =
            "The specified CSV file either does not exist or is not in an allowed path.";

    private final Config config;
    private final AllowedAuxiliaryPathChecker pathChecker;
    private final AtomicReference<Map<String, String>> lookupRef = new AtomicReference<>(ImmutableMap.of());
    private final String name;

    private FileInfo fileInfo = FileInfo.empty();

    @Inject
    public CSVFileDataAdapter(@Assisted("id") String id,
                              @Assisted("name") String name,
                              @Assisted LookupDataAdapterConfiguration config,
                              MetricRegistry metricRegistry,
                              AllowedAuxiliaryPathChecker pathChecker) {
        super(id, name, config, metricRegistry);
        this.name = name;
        this.config = (Config) config;
        this.pathChecker = pathChecker;
    }

    @Override
    public void doStart() throws Exception {
        LOG.debug("Starting CSV data adapter for file: {}", config.path());
        if (isNullOrEmpty(config.path())) {
            throw new IllegalStateException("File path needs to be set");
        }
        if (!pathChecker.fileIsInAllowedPath(Paths.get(config.path()))) {
            throw new IllegalStateException(ALLOWED_PATH_ERROR);
        }
        if (config.checkInterval() < 1) {
            throw new IllegalStateException("Check interval setting cannot be smaller than 1");
        }

        // Set file info before parsing the data for the first time
        fileInfo = getNewFileInfo();
        lookupRef.set(parseCSVFile());
    }

    @Override
    public Duration refreshInterval() {
        return Duration.standardSeconds(Ints.saturatedCast(config.checkInterval()));
    }

    @Override
    protected void doRefresh(LookupCachePurge cachePurge) throws Exception {
        if (!pathChecker.fileIsInAllowedPath(Paths.get(config.path()))) {
            LOG.error(ALLOWED_PATH_ERROR);
            setError(new IllegalStateException(ALLOWED_PATH_ERROR));
            return;
        }

        if (!Files.isReadable(Paths.get(config.path()))) {
            String error = f("The specified file [%s] does not exist or is not readable. " +
                            "To resolve this error, edit the adapter [%s] and specify a new path, or restore the file " +
                            "or read access to it.",
                    config.path(), name);
            LOG.error(error);
            setError(new IllegalStateException(error));
            return;
        }

        try {
            final FileInfo.Change fileChanged = fileInfo.checkForChange();
            if (!fileChanged.isChanged() && !getError().isPresent()) {
                // Nothing to do, file did not change
                return;
            }

            LOG.debug("CSV file {} has changed, updating data", config.path());
            lookupRef.set(parseCSVFile());
            cachePurge.purgeAll();
            // If the file has been moved, then moved back, the fileInfo might have been disconnected.
            // In this case, create a new fileInfo.
            fileInfo = fileChanged.fileInfo() != null ? fileChanged.fileInfo() : getNewFileInfo();
            clearError();
        } catch (IOException e) {
            LOG.error("Couldn't check data adapter <{}> CSV file {} for updates: {} {}", name(), config.path(), e.getClass().getCanonicalName(), e.getMessage());
            setError(e);
        }
    }

    private Map<String, String> parseCSVFile() throws IOException {
        final InputStream inputStream = Files.newInputStream(Paths.get(config.path()));
        final InputStreamReader fileReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        final ImmutableMap.Builder<String, String> newLookupBuilder = ImmutableMap.builder();

        try (final CSVReader csvReader = new CSVReader(fileReader, config.separatorAsChar(), config.quotecharAsChar())) {
            int line = 0;
            int keyColumn = -1;
            int valueColumn = -1;

            while (true) {
                final String[] next = csvReader.readNext();
                if (next == null) {
                    break;
                }
                line++;

                if (line == 1) {
                    // The first line in the CSV file provides the column names
                    int col = 0;
                    for (final String column : next) {
                        if (!isNullOrEmpty(column)) {
                            if (config.keyColumn().equals(column)) {
                                keyColumn = col;
                            }
                            if (config.valueColumn().equals(column)) {
                                valueColumn = col;
                            }
                        }
                        col++;
                    }
                } else {
                    // The other lines are supposed to be data entries
                    if (keyColumn < 0 || valueColumn < 0) {
                        throw new IllegalStateException("Couldn't detect column number for key or value - check CSV file format");
                    }
                    if (next.length == 1 && StringUtils.isEmpty(next[0])) {
                        LOG.debug("Skipping empty line in CSV adapter file [{}/{}].", name, config.path());
                        continue;
                    }

                    final String value;
                    final String key;
                    try {
                        key = next[keyColumn];
                        value = next[valueColumn];
                    } catch (IndexOutOfBoundsException e) {
                        final String error = f("The CSV file [%s] contains invalid lines. Please check the file and ensure " +
                                "that both key and value columns are present in all lines.", name);
                        throw new IllegalStateException(error, e);
                    }

                    if (!config.isCidrLookup()) {
                        if (config.isCaseInsensitiveLookup()) {
                            newLookupBuilder.put(key.toLowerCase(Locale.ENGLISH), value);
                        } else {
                            newLookupBuilder.put(key, value);
                        }
                    } else {
                        Optional<IpSubnet> optSubnet = ReservedIpChecker.stringToSubnet(key);
                        if (optSubnet.isPresent()) {
                            newLookupBuilder.put(key, value);
                        } else {
                            // If key in a CIDR lookup adapter is not already a valid CIDR range, check if it is an IP
                            String cidr = ipAddressToCIDR(key);
                            if (cidr != null) {
                                newLookupBuilder.put(cidr, value);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Couldn't parse CSV file {} (settings separator=<{}> quotechar=<{}> key_column=<{}> value_column=<{}>)", config.path(),
                    config.separator(), config.quotechar(), config.keyColumn(), config.valueColumn(), e);
            setError(e);
            throw new IllegalStateException(e);
        }

        return newLookupBuilder.build();
    }

    private String ipAddressToCIDR(String ip) {
        String cidr = null;
        try {
            InetAddress address = InetAddresses.forString(ip);
            if (address instanceof Inet4Address) {
                cidr = f("%s/32", ip);
            } else if (address instanceof Inet6Address) {
                cidr = f("%s/128", ip);
            }
        } catch (IllegalArgumentException ignored) {
            LOG.warn("Key <{}> in CIDR lookup CSV data adapter <{}> is not a valid CIDR or IP address. Skipping invalid line.", ip, name);
        }
        return cidr;
    }

    private FileInfo getNewFileInfo() {
        return FileInfo.forPath(Paths.get(config.path()));
    }

    @Override
    public void doStop() throws Exception {
        LOG.debug("Stopping CSV data adapter for file: {}", config.path());
    }

    @Override
    public LookupResult doGet(Object key) {
        if (config.isCidrLookup()) {
            return getResultForCIDRRange(key);
        }
        final String stringKey = config.isCaseInsensitiveLookup() ? String.valueOf(key).toLowerCase(Locale.ENGLISH) : String.valueOf(key);
        final String value = lookupRef.get().get(stringKey);

        if (value == null) {
            return getEmptyResult();
        }

        return LookupResult.single(value);
    }

    public LookupResult getResultForCIDRRange(Object ip) {
        LookupResult result = getEmptyResult();
        try {
            // Convert directly to InetAddress to avoid long timeouts using name service lookups
            InetAddress address = InetAddresses.forString(String.valueOf(ip));
            int longestMatch = 0;
            for (Map.Entry<String, String> entry : lookupRef.get().entrySet()) {
                String range = entry.getKey();
                Optional<IpSubnet> optSubnet = ReservedIpChecker.stringToSubnet(range);
                if (optSubnet.isEmpty()) {
                    LOG.debug("CIDR range '{}' in data adapter '{}' is not a valid subnet, skipping this key in lookup.", entry, name);
                } else {
                    IpSubnet subnet = optSubnet.get();
                    if (subnet.contains(address) && (result.isEmpty() || longestMatch < subnet.getPrefixLength())) {
                        longestMatch = subnet.getPrefixLength();
                        result = LookupResult.single(entry.getValue());
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            LOG.debug("Attempted to do a CIDR range lookup on invalid IP '{}'", ip);
            return getErrorResult();
        }

        return result;
    }

    @Override
    public void set(Object key, Object value) {

    }

    public interface Factory extends LookupDataAdapter.Factory<CSVFileDataAdapter> {
        @Override
        CSVFileDataAdapter create(@Assisted("id") String id,
                                  @Assisted("name") String name,
                                  LookupDataAdapterConfiguration configuration);

        @Override
        Descriptor getDescriptor();
    }

    public static class Descriptor extends LookupDataAdapter.Descriptor<Config> {
        public Descriptor() {
            super(NAME, Config.class);
        }

        @Override
        public Config defaultConfiguration() {
            return Config.builder()
                    .type(NAME)
                    .path("/etc/graylog/lookup-table.csv")
                    .separator(",")
                    .quotechar("\"")
                    .keyColumn("key")
                    .valueColumn("value")
                    .checkInterval(60)
                    .caseInsensitiveLookup(false)
                    .cidrLookup(false)
                    .build();
        }
    }

    @AutoValue
    @WithBeanGetter
    @JsonAutoDetect
    @JsonDeserialize(builder = AutoValue_CSVFileDataAdapter_Config.Builder.class)
    @JsonTypeName(NAME)
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    public static abstract class Config implements LookupDataAdapterConfiguration {

        @Override
        @JsonProperty(TYPE_FIELD)
        public abstract String type();

        @JsonProperty("path")
        @NotEmpty
        public abstract String path();

        // Using String here instead of char to allow deserialization of a longer (invalid) string to get proper
        // validation error messages
        @JsonProperty("separator")
        @Size(min = 1, max = 1)
        @NotEmpty
        public abstract String separator();

        @JsonIgnore
        public char separatorAsChar() {
            return separator().charAt(0);
        }

        // Using String here instead of char to allow deserialization of a longer (invalid) string to get proper
        // validation error messages
        @JsonProperty("quotechar")
        @Size(min = 1, max = 1)
        @NotEmpty
        public abstract String quotechar();

        @JsonIgnore
        public char quotecharAsChar() {
            return quotechar().charAt(0);
        }

        @JsonProperty("key_column")
        @NotEmpty
        public abstract String keyColumn();

        @JsonProperty("value_column")
        @NotEmpty
        public abstract String valueColumn();

        @JsonProperty("check_interval")
        @Min(1)
        public abstract long checkInterval();

        @JsonProperty("case_insensitive_lookup")
        public abstract Optional<Boolean> caseInsensitiveLookup();

        @JsonProperty("cidr_lookup")
        public abstract Optional<Boolean> cidrLookup();

        public boolean isCaseInsensitiveLookup() {
            return caseInsensitiveLookup().isPresent() && caseInsensitiveLookup().get();
        }

        public boolean isCidrLookup() {
            return cidrLookup().isPresent() && cidrLookup().get();
        }

        public static Builder builder() {
            return new AutoValue_CSVFileDataAdapter_Config.Builder();
        }

        @Override
        public Optional<Multimap<String, String>> validate(LookupDataAdapterValidationContext context) {
            final ArrayListMultimap<String, String> errors = ArrayListMultimap.create();

            final Path path = Paths.get(path());
            if (!context.getPathChecker().fileIsInAllowedPath(path)) {
                errors.put("path", ALLOWED_PATH_ERROR);

                // Intentionally return here, because in the Cloud context, we should not perform the following checks
                // to report to the user whether or not a file exists.
                return Optional.of(errors);
            }

            if (!Files.exists(path)) {
                errors.put("path", "The file does not exist.");
            } else if (!Files.isReadable(path)) {
                errors.put("path", "The file cannot be read.");
            }

            return errors.isEmpty() ? Optional.empty() : Optional.of(errors);
        }

        @Override
        public boolean isCloudCompatible() {
            return false;
        }

        @AutoValue.Builder
        public abstract static class Builder {
            @JsonProperty(TYPE_FIELD)
            public abstract Builder type(String type);

            @JsonProperty("path")
            public abstract Builder path(String path);

            @JsonProperty("separator")
            public abstract Builder separator(String separator);

            @JsonProperty("quotechar")
            public abstract Builder quotechar(String quotechar);

            @JsonProperty("key_column")
            public abstract Builder keyColumn(String keyColumn);

            @JsonProperty("value_column")
            public abstract Builder valueColumn(String valueColumn);

            @JsonProperty("check_interval")
            public abstract Builder checkInterval(long checkInterval);

            @JsonProperty("case_insensitive_lookup")
            public abstract Builder caseInsensitiveLookup(Boolean caseInsensitiveLookup);

            @JsonProperty("cidr_lookup")
            public abstract Builder cidrLookup(Boolean cidrLookup);

            public abstract Config build();
        }
    }
}
