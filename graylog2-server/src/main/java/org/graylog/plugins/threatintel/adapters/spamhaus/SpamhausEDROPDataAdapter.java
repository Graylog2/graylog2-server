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
package org.graylog.plugins.threatintel.adapters.spamhaus;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.inject.assistedinject.Assisted;
import org.apache.commons.net.util.SubnetUtils;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog.plugins.threatintel.PluginConfigService;
import org.graylog.plugins.threatintel.tools.AdapterDisabledException;
import org.graylog2.lookup.adapters.dsvhttp.HTTPFileRetriever;
import org.graylog2.plugin.lookup.LookupCachePurge;
import org.graylog2.plugin.lookup.LookupDataAdapter;
import org.graylog2.plugin.lookup.LookupDataAdapterConfiguration;
import org.graylog2.plugin.lookup.LookupResult;
import org.graylog2.shared.utilities.ExceptionUtils;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import jakarta.validation.constraints.Min;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;

public class SpamhausEDROPDataAdapter extends LookupDataAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(SpamhausEDROPDataAdapter.class);
    public static final String NAME = "spamhaus-edrop";

    private static final String[] lists = {
            "https://www.spamhaus.org/drop/drop.txt",
            "https://www.spamhaus.org/drop/edrop.txt"
    };

    // Update the list at most once an hour: https://www.spamhaus.org/faq/section/DROP%20FAQ#218
    // the current cache-control header says max-age 14400 seconds
    private static final Duration REFRESH_INTERVAL = Duration.standardHours(4);

    private final AtomicReference<Map<String, Map<SubnetUtils.SubnetInfo, String>>> subnets = new AtomicReference<>(Collections.emptyMap());
    private final HTTPFileRetriever httpFileRetriever;
    private final PluginConfigService pluginConfigService;

    @Inject
    public SpamhausEDROPDataAdapter(@Assisted("id") String id,
                                    @Assisted("name") String name,
                                    @Assisted LookupDataAdapterConfiguration config,
                                    MetricRegistry metricRegistry,
                                    HTTPFileRetriever httpFileRetriever,
                                    PluginConfigService pluginConfigService) {
        super(id, name, config, metricRegistry);
        this.httpFileRetriever = httpFileRetriever;
        this.pluginConfigService = pluginConfigService;
    }

    @Override
    public void doStart() throws Exception {
        if (!pluginConfigService.config().getCurrent().spamhausEnabled()) {
            throw new AdapterDisabledException("Spamhaus service is disabled, not starting (E)DROP adapter. To enable it please go to System / Configurations.");
        }
        final ImmutableMap.Builder<String, Map<SubnetUtils.SubnetInfo, String>> builder = ImmutableMap.builder();
        for (String list : lists) {
            final Map<SubnetUtils.SubnetInfo, String> subnetMap = fetchSubnetsFromEDROPLists(list);
            if (subnetMap != null) {
                builder.put(list, subnetMap);
            }
        }
        this.subnets.set(builder.build());
    }

    @Override
    protected void doStop() throws Exception {
        // nothing to do
    }

    @Override
    public Duration refreshInterval() {
        if (!pluginConfigService.config().getCurrent().spamhausEnabled()) {
            return Duration.ZERO;
        }
        return Duration.standardSeconds(((Config) getConfig()).refreshInterval());
    }

    @Override
    protected void doRefresh(LookupCachePurge cachePurge) throws Exception {
        if (!pluginConfigService.config().getCurrent().spamhausEnabled()) {
            throw new AdapterDisabledException("Spamhaus service is disabled, not refreshing (E)DROP adapter. To enable it please go to System / Configurations.");
        }
        // keep the old results, which will get overridden if we can fetch new lists
        final Map<String, Map<SubnetUtils.SubnetInfo, String>> result = new HashMap<>(this.subnets.get());
        boolean hasUpdates = false;
        for (String list : lists) {
            final Map<SubnetUtils.SubnetInfo, String> newList = fetchSubnetsFromEDROPLists(list);
            if (newList != null) {
                result.put(list, newList);
                hasUpdates = true;
            }
        }
        // no changes in the lists, we don't have to purge or update our subnets
        if (!hasUpdates) {
            return;
        }
        this.subnets.set(ImmutableMap.copyOf(result));
        cachePurge.purgeAll();
    }

    private Map<SubnetUtils.SubnetInfo, String> fetchSubnetsFromEDROPLists(String list) {
        final ImmutableMap.Builder<SubnetUtils.SubnetInfo, String> builder = ImmutableMap.builder();
        try {
            final Optional<String> body = httpFileRetriever.fetchFileIfNotModified(list);
            if (body.isPresent()) {
                try (final Scanner scanner = new Scanner(body.get())) {
                    while (scanner.hasNextLine()) {
                        final String line = scanner.nextLine().trim();

                        if (!line.isEmpty() && !line.startsWith(";") && line.contains(";")) {
                            final String[] parts = line.split(";");

                            final SubnetUtils su = new SubnetUtils(parts[0].trim());
                            builder.put(su.getInfo(), parts.length > 1 ? parts[1].trim() : "N/A");
                        }
                    }
                }
            } else {
                return null;
            }
        } catch (IOException e) {
            LOG.error("Unable to retrieve Spamhaus (E)DROP list <" + list + ">: ", e);
            return null;
        }

        return builder.build();
    }

    @Override
    public LookupResult doGet(Object key) {
        final String ip = String.valueOf(key);

        if (this.subnets.get().isEmpty()) {
            return LookupResult.empty();
        }

        // TODO potentially use a RangeMap here instead of doing linear searches all the time
        // We should be able to use ranges of the integer values of lowAddress and highAddress as keys
        final Optional<Map.Entry<SubnetUtils.SubnetInfo, String>> match;
        try {
            match = subnets.get().values()
                    .stream()
                    .flatMap(list -> list.entrySet().stream())
                    .filter(entry -> entry.getKey().isInRange(ip))
                    .findFirst();
        } catch (IllegalArgumentException e) {
            // Gracefully handle the case when a blank or invalid IP is supplied.
            LOG.debug("[{}] is an invalid IP address. Lookup aborted. {}", ip, ExceptionUtils.getRootCauseMessage(e));
            return LookupResult.empty();
        }

        return match.map(entry -> LookupResult.multi(true,
                ImmutableMap.of("sbl_id", entry.getValue(), "subnet", entry.getKey().getCidrSignature())
        )).orElse(LookupResult.single(false));
    }

    @Override
    public void set(Object key, Object value) {

    }

    public interface Factory extends LookupDataAdapter.Factory<SpamhausEDROPDataAdapter> {
        @Override
        SpamhausEDROPDataAdapter create(@Assisted("id") String id,
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
                    .refreshInterval(REFRESH_INTERVAL.toStandardSeconds().getSeconds())
                    .build();
        }
    }

    @AutoValue
    @WithBeanGetter
    @JsonAutoDetect
    @JsonDeserialize(builder = AutoValue_SpamhausEDROPDataAdapter_Config.Builder.class)
    @JsonTypeName(NAME)
    public static abstract class Config implements LookupDataAdapterConfiguration {

        @Override
        @JsonProperty(TYPE_FIELD)
        public abstract String type();

        // refresh interval should not be shorter than an hour per spamhaus rules
        @JsonProperty("refresh_interval")
        @Min(3600)
        public abstract long refreshInterval();

        public static Builder builder() {
            return new AutoValue_SpamhausEDROPDataAdapter_Config.Builder();
        }

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

            public abstract Config build();
        }
    }
}
