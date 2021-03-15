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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.primitives.Ints;
import com.google.inject.assistedinject.Assisted;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.lookup.AllowedAuxiliaryPathChecker;
import org.graylog2.plugin.lookup.LookupCachePurge;
import org.graylog2.plugin.lookup.LookupDataAdapter;
import org.graylog2.plugin.lookup.LookupDataAdapterConfiguration;
import org.graylog2.plugin.lookup.LookupResult;
import org.graylog2.plugin.utilities.FileInfo;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Strings.isNullOrEmpty;

public class CSVFileDataAdapter extends LookupDataAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(CSVFileDataAdapter.class);

    public static final String NAME = "csvfile";

    private final Config config;
    private final AllowedAuxiliaryPathChecker pathChecker;
    private final AtomicReference<Map<String, String>> lookupRef = new AtomicReference<>(ImmutableMap.of());

    private FileInfo fileInfo = FileInfo.empty();

    @Inject
    public CSVFileDataAdapter(@Assisted("id") String id,
                              @Assisted("name") String name,
                              @Assisted LookupDataAdapterConfiguration config,
                              MetricRegistry metricRegistry,
                              AllowedAuxiliaryPathChecker pathChecker) {
        super(id, name, config, metricRegistry);
        this.config = (Config) config;
        this.pathChecker = pathChecker;
    }

    @Override
    public void doStart() throws Exception {
        LOG.debug("Starting CSV data adapter for file: {}", config.path());
        if (isNullOrEmpty(config.path())) {
            throw new IllegalStateException("File path needs to be set");
        }
        if (!pathChecker.isInAllowedPath(config.path())) {
            throw new IllegalStateException("The specified CSV file is not in a trusted path.");
        }
        if (config.checkInterval() < 1) {
            throw new IllegalStateException("Check interval setting cannot be smaller than 1");
        }

        // Set file info before parsing the data for the first time
        fileInfo = FileInfo.forPath(Paths.get(config.path()));
        lookupRef.set(parseCSVFile());
    }

    @Override
    public Duration refreshInterval() {
        return Duration.standardSeconds(Ints.saturatedCast(config.checkInterval()));
    }

    @Override
    protected void doRefresh(LookupCachePurge cachePurge) throws Exception {
        try {
            final FileInfo.Change fileChanged = fileInfo.checkForChange();
            if (!fileChanged.isChanged() && !getError().isPresent()) {
                // Nothing to do, file did not change
                return;
            }

            LOG.debug("CSV file {} has changed, updating data", config.path());
            lookupRef.set(parseCSVFile());
            cachePurge.purgeAll();
            fileInfo = fileChanged.fileInfo();
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
                    if (config.isCaseInsensitiveLookup()) {
                        newLookupBuilder.put(next[keyColumn].toLowerCase(Locale.ENGLISH), next[valueColumn]);
                    } else {
                        newLookupBuilder.put(next[keyColumn], next[valueColumn]);
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Couldn't parse CSV file {} (settings separator=<{}> quotechar=<{}> key_column=<{}> value_column=<{}>)", config.path(),
                    config.separator(), config.quotechar(), config.keyColumn(), config.valueColumn(), e);
            setError(e);
        }

        return newLookupBuilder.build();
    }

    @Override
    public void doStop() throws Exception {
        LOG.debug("Stopping CSV data adapter for file: {}", config.path());
    }

    @Override
    public LookupResult doGet(Object key) {
        final String stringKey = config.isCaseInsensitiveLookup() ? String.valueOf(key).toLowerCase(Locale.ENGLISH) : String.valueOf(key);
        final String value = lookupRef.get().get(stringKey);

        if (value == null) {
            return getEmptyResult();
        }

        return LookupResult.single(value);
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
                    .build();
        }
    }

    @AutoValue
    @WithBeanGetter
    @JsonAutoDetect
    @JsonDeserialize(builder = AutoValue_CSVFileDataAdapter_Config.Builder.class)
    @JsonTypeName(NAME)
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

        public boolean isCaseInsensitiveLookup() {
            return caseInsensitiveLookup().isPresent() && caseInsensitiveLookup().get();
        }

        public static Builder builder() {
            return new AutoValue_CSVFileDataAdapter_Config.Builder();
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

            public abstract Config build();
        }
    }
}
