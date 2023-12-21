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
package org.graylog.plugins.threatintel.whois.ip;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.inject.assistedinject.Assisted;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.lookup.LookupCachePurge;
import org.graylog2.plugin.lookup.LookupDataAdapter;
import org.graylog2.plugin.lookup.LookupDataAdapterConfiguration;
import org.graylog2.plugin.lookup.LookupResult;
import org.joda.time.Duration;

import jakarta.inject.Inject;

import java.util.Map;
import java.util.Optional;

public class WhoisDataAdapter extends LookupDataAdapter {
    public static final String NAME = "whois";
    public static final String ORGANIZATION_FIELD = "organization";
    public static final String COUNTRY_CODE_FIELD = "country_code";

    private final WhoisIpLookup whoisIpLookup;

    @Inject
    public WhoisDataAdapter(@Assisted("id") String id,
                            @Assisted("name") String name,
                            @Assisted LookupDataAdapterConfiguration config,
                            MetricRegistry metricRegistry) {
        super(id, name, config, metricRegistry);
        this.whoisIpLookup = new WhoisIpLookup((Config) config, metricRegistry);
    }

    @Override
    protected void doStart() throws Exception {
    }

    @Override
    protected void doStop() throws Exception {
    }

    @Override
    public Duration refreshInterval() {
        return Duration.ZERO;
    }

    @Override
    protected void doRefresh(LookupCachePurge cachePurge) throws Exception {
    }

    @Override
    protected LookupResult doGet(Object key) {
        try {
            final WhoisIpLookupResult result = this.whoisIpLookup.run(key.toString());
            if (!WhoisIpLookupResult.empty().equals(result)) {
                final Map<Object, Object> fields = ImmutableMap.of(
                        ORGANIZATION_FIELD, result.getOrganization(),
                        COUNTRY_CODE_FIELD, result.getCountryCode()
                );
                return LookupResult.multi(result.getOrganization() + "/" + result.getCountryCode(), fields);
            } else {
                return LookupResult.empty();
            }
        } catch (Exception e) {
            return LookupResult.single("Lookup Error: " + e.getMessage());
        }
    }

    @Override
    public void set(Object key, Object value) {
        throw new UnsupportedOperationException();
    }

    public interface Factory extends LookupDataAdapter.Factory<WhoisDataAdapter> {
        @Override
        WhoisDataAdapter create(@Assisted("id") String id,
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
                    .registry(InternetRegistry.ARIN)
                    .connectTimeout(1000)
                    .readTimeout(1000)
                    .build();
        }
    }

    @AutoValue
    @WithBeanGetter
    @JsonAutoDetect
    @JsonDeserialize(builder = Config.Builder.class)
    @JsonTypeName(NAME)
    public static abstract class Config implements LookupDataAdapterConfiguration {

        @Override
        @JsonProperty(TYPE_FIELD)
        public abstract String type();

        @JsonProperty("registry")
        public abstract InternetRegistry registry();

        @JsonProperty("connect_timeout")
        public abstract int connectTimeout();

        @JsonProperty("read_timeout")
        public abstract int readTimeout();

        public static Builder builder() {
            return new AutoValue_WhoisDataAdapter_Config.Builder();
        }

        @Override
        public Optional<Multimap<String, String>> validate() {
            return Optional.empty();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            @JsonCreator
            public static Builder create() {
                return Config.builder().connectTimeout(1000).readTimeout(1000);
            }

            @JsonProperty(TYPE_FIELD)
            public abstract Builder type(String type);

            @JsonProperty("registry")
            public abstract Builder registry(InternetRegistry registry);

            @JsonProperty("connect_timeout")
            public abstract Builder connectTimeout(int connectTimeout);

            @JsonProperty("read_timeout")
            public abstract Builder readTimeout(int readTimeout);

            abstract Config autoBuild();

            public Config build() {
                // TODO: For now we always use ARIN regardless of what the user configured. This is because we can only
                // TODO: process redirects from ARIN responses for now. See: https://github.com/Graylog2/graylog-plugin-threatintel/issues/77
                registry(InternetRegistry.ARIN);
                return autoBuild();
            }
        }
    }
}
