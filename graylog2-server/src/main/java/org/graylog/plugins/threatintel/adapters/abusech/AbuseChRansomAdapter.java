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
package org.graylog.plugins.threatintel.adapters.abusech;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.assistedinject.Assisted;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog.plugins.threatintel.tools.AdapterDisabledException;
import org.graylog2.plugin.lookup.LookupCachePurge;
import org.graylog2.plugin.lookup.LookupDataAdapter;
import org.graylog2.plugin.lookup.LookupDataAdapterConfiguration;
import org.graylog2.plugin.lookup.LookupResult;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.Min;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class AbuseChRansomAdapter extends LookupDataAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(AbuseChRansomAdapter.class);

    // The abuse.ch Ransomware Tracker was shut down on 2019-12-08.
    // This Data Adapter is no longer useful.

    private static final int REFRESH_INTERVAL = Period.minutes(5).toStandardSeconds().getSeconds() / 2;

    public static final String NAME = "abuse-ch-ransom";

    @Inject
    public AbuseChRansomAdapter(@Assisted("id") String id,
                                @Assisted("name") String name,
                                @Assisted LookupDataAdapterConfiguration config,
                                MetricRegistry metricRegistry) {
        super(id, name, config, metricRegistry);
    }

    @Override
    public void doStart() throws Exception {
        throw new AdapterDisabledException("The abuse.ch Ransomware Tracker was shut down on 2019-12-08. This Data Adapter should be deleted.");
    }

    @Override
    protected void doStop() throws Exception {
        // nothing to do
    }

    @Override
    public Duration refreshInterval() {
        return Duration.ZERO;
    }

    @Override
    protected void doRefresh(LookupCachePurge cachePurge) throws Exception {
        throw new AdapterDisabledException("The abuse.ch Ransomware Tracker was shut down on 2019-12-08. This Data Adapter should be deleted.");
    }

    @Override
    protected LookupResult doGet(Object key) {
        return LookupResult.empty();
    }

    @Override
    public void set(Object key, Object value) {
        // not supported
    }

    public interface Factory extends LookupDataAdapter.Factory<AbuseChRansomAdapter> {
        @Override
        AbuseChRansomAdapter create(@Assisted("id") String id,
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
                    .refreshInterval(REFRESH_INTERVAL)
                    .blocklistType(BlocklistType.DOMAINS)
                    .build();
        }
    }

    @AutoValue
    @WithBeanGetter
    @JsonAutoDetect
    @JsonDeserialize(builder = AutoValue_AbuseChRansomAdapter_Config.Builder.class)
    @JsonTypeName(NAME)
    public static abstract class Config implements LookupDataAdapterConfiguration {

        public static Builder builder() {
            return new AutoValue_AbuseChRansomAdapter_Config.Builder();
        }

        @Override
        @JsonProperty(TYPE_FIELD)
        public abstract String type();

        @JsonProperty("refresh_interval")
        @Min(150) // see REFRESH_INTERVAL
        public abstract long refreshInterval();

        @Nullable
        @JsonProperty("refresh_interval_unit")
        public abstract TimeUnit refreshIntervalUnit();

        @JsonProperty("blocklist_type")
        public abstract BlocklistType blocklistType();

        @Override
        public Optional<Multimap<String, String>> validate() {
            final ArrayListMultimap<String, String> errors = ArrayListMultimap.create();

            return errors.isEmpty() ? Optional.empty() : Optional.of(errors);
        }

        @AutoValue.Builder
        public abstract static class Builder {
            @JsonProperty(TYPE_FIELD)
            public abstract Builder type(String type);

            @JsonProperty("refresh_interval")
            public abstract Builder refreshInterval(long refreshInterval);

            @JsonProperty("blocklist_type")
            public abstract Builder blocklistType(BlocklistType blocklistType);

            @JsonProperty("refresh_interval_unit")
            public abstract Builder refreshIntervalUnit(@Nullable TimeUnit refreshIntervalUnit);

            public abstract Config build();
        }
    }
}
