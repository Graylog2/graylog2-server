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
package org.graylog.plugins.threatintel.adapters.tor;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.Multimap;
import com.google.inject.assistedinject.Assisted;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog.plugins.threatintel.PluginConfigService;
import org.graylog.plugins.threatintel.tools.AdapterDisabledException;
import org.graylog2.plugin.lookup.LookupCachePurge;
import org.graylog2.plugin.lookup.LookupDataAdapter;
import org.graylog2.plugin.lookup.LookupDataAdapterConfiguration;
import org.graylog2.plugin.lookup.LookupResult;
import org.joda.time.Duration;

import jakarta.inject.Inject;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

public class TorExitNodeDataAdapter extends LookupDataAdapter {
    public static final String NAME = "torexitnode";
    private final OkHttpClient client;
    private final TorExitNodeListParser parser;
    private final PluginConfigService pluginConfigService;
    private Map<String, List<String>> torExitNodes = Collections.emptyMap();

    @Inject
    public TorExitNodeDataAdapter(
            @Assisted("id") String id,
            @Assisted("name") String name,
            @Assisted LookupDataAdapterConfiguration config,
            MetricRegistry metricRegistry,
            TorExitNodeListParser torExitNodeListParser,
            OkHttpClient httpClient,
            PluginConfigService pluginConfigService) {
        super(id, name, config, metricRegistry);

        this.client = httpClient.newBuilder()
                .followRedirects(true)
                .followSslRedirects(true)
                .build();

        this.parser = torExitNodeListParser;
        this.pluginConfigService = pluginConfigService;
    }

    public interface Factory extends LookupDataAdapter.Factory<TorExitNodeDataAdapter> {
        @Override
        TorExitNodeDataAdapter create(@Assisted("id") String id,
                                      @Assisted("name") String name,
                                      LookupDataAdapterConfiguration configuration);

        @Override
        Descriptor getDescriptor();
    }

    @Override
    protected void doStart() throws Exception {
        if (!pluginConfigService.config().getCurrent().torEnabled()) {
            throw new AdapterDisabledException("TOR service is disabled, not starting TOR exit addresses adapter. To enable it please go to System / Configurations.");
        }
        final Response torExitNodeListResponse = this.client.newCall(new Request.Builder()
                        .get()
                        .url(new HttpUrl.Builder()
                                .scheme("https")
                                .host("check.torproject.org")
                                .addPathSegment("exit-addresses")
                                .build())
                        .build())
                .execute();

        final ResponseBody body = torExitNodeListResponse.body();
        if (torExitNodeListResponse.isSuccessful() && body != null) {
            this.torExitNodes = this.parser.parse(body.string());
        }
    }

    @Override
    protected void doStop() throws Exception {
    }

    @Override
    public Duration refreshInterval() {
        if (!pluginConfigService.config().getCurrent().torEnabled()) {
            return Duration.ZERO;
        }
        return Duration.standardMinutes(60);
    }

    @Override
    protected void doRefresh(LookupCachePurge cachePurge) throws Exception {
        doStart();
        cachePurge.purgeAll();
    }

    @Override
    protected LookupResult doGet(Object key) {
        final List<String> value = this.torExitNodes.get(key.toString());
        if (value != null) {
            final StringJoiner stringJoiner = new StringJoiner(", ");
            value.forEach(stringJoiner::add);
            return LookupResult.multi(stringJoiner.toString(), Map.of("node_ids", value));
        } else {
            return LookupResult.empty();
        }
    }

    @Override
    public void set(Object key, Object value) {
        throw new UnsupportedOperationException();
    }

    public static class Descriptor extends LookupDataAdapter.Descriptor<Config> {
        public Descriptor() {
            super(NAME, Config.class);
        }

        @Override
        public Config defaultConfiguration() {
            return Config.builder()
                    .type(NAME)
                    .build();
        }
    }

    @AutoValue
    @WithBeanGetter
    @JsonAutoDetect
    @JsonDeserialize(builder = AutoValue_TorExitNodeDataAdapter_Config.Builder.class)
    @JsonTypeName(NAME)
    public static abstract class Config implements LookupDataAdapterConfiguration {

        @Override
        @JsonProperty(TYPE_FIELD)
        public abstract String type();

        public static Builder builder() {
            return new AutoValue_TorExitNodeDataAdapter_Config.Builder();
        }

        @Override
        public Optional<Multimap<String, String>> validate() {
            return Optional.empty();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            @JsonProperty(TYPE_FIELD)
            public abstract Builder type(String type);

            public abstract Config build();
        }
    }
}
