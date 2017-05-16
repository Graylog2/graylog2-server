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
package org.graylog2.lookup.adapters;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.primitives.Ints;
import com.google.inject.assistedinject.Assisted;

import au.com.bytecode.opencsv.CSVReader;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.lookup.LookupDataAdapter;
import org.graylog2.plugin.lookup.LookupDataAdapterConfiguration;
import org.graylog2.plugin.lookup.LookupResult;
import org.graylog2.plugin.utilities.FileInfo;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

import static com.google.common.base.Strings.isNullOrEmpty;

public class CSVFileDataAdapter extends LookupDataAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(CSVFileDataAdapter.class);

    public static final String NAME = "csvfile";

    private final Config config;
    private final AtomicReference<Map<String, String>> lookupRef = new AtomicReference<>(ImmutableMap.of());

    private FileInfo fileInfo;

    @Inject
    public CSVFileDataAdapter(@Named("daemonScheduler") ScheduledExecutorService scheduler,
                              @Assisted("id") String id,
                              @Assisted("name") String name,
                              @Assisted LookupDataAdapterConfiguration config) {
        super(id, name, config, scheduler);
        this.config = (Config) config;
    }

    @Override
    public void doStart() throws Exception {
        LOG.debug("Starting CSV data adapter for file: {}", config.path());
        if (isNullOrEmpty(config.path())) {
            throw new IllegalStateException("File path needs to be set");
        }

        try {
            // Set file info before parsing the data for the first time
            fileInfo = FileInfo.forPath(Paths.get(config.path()));
            lookupRef.set(parseCSVFile());

        } catch (Exception e) {
            setError(e);
        }
        if (config.checkInterval() < 1) {
            throw new IllegalStateException("Check interval setting cannot be smaller than 1");
        }
    }

    @Override
    protected Duration refreshInterval() {
        return Duration.standardSeconds(Ints.saturatedCast(config.checkInterval()));
    }

    @Override
    protected void doRefresh() throws Exception {
        try {
            clearError();
            final FileInfo.Change fileChanged = fileInfo.checkForChange();
            if (!fileChanged.isChanged()) {
                // Nothing to do, file did not change
                return;
            }

            LOG.debug("CSV file {} has changed, updating data", config.path());
            lookupRef.set(parseCSVFile());
            getLookupTable().cache().purge();
            fileInfo = fileChanged.fileInfo();
        } catch (IOException e) {
            LOG.error("Couldn't check CSV file {} for updates", config.path(), e);
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
                            } else if (config.valueColumn().equals(column)) {
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
                    newLookupBuilder.put(next[keyColumn], next[valueColumn]);
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
        final String value = lookupRef.get().get(String.valueOf(key));

        if (value == null) {
            return LookupResult.empty();
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

        public static Builder builder() {
            return new AutoValue_CSVFileDataAdapter_Config.Builder();
        }

        @Override
        public Optional<Multimap<String, String>> validate() {
            final ArrayListMultimap<String, String> errors = ArrayListMultimap.create();

            final Path path = Paths.get(path());
            if (!Files.exists(path)) {
                errors.put("path", "The file does not exist");
            } else if (!Files.isReadable(path)) {
                errors.put("path", "The file cannot be read");
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

            public abstract Config build();
        }
    }
}
