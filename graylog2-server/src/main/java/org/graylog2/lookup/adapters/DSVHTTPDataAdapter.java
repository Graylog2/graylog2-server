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

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.primitives.Ints;
import com.google.inject.assistedinject.Assisted;
import okhttp3.HttpUrl;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.lookup.adapters.dsvhttp.DSVParser;
import org.graylog2.lookup.adapters.dsvhttp.HTTPFileRetriever;
import org.graylog2.plugin.lookup.LookupCachePurge;
import org.graylog2.plugin.lookup.LookupDataAdapter;
import org.graylog2.plugin.lookup.LookupDataAdapterConfiguration;
import org.graylog2.plugin.lookup.LookupResult;
import org.graylog2.system.urlwhitelist.UrlNotWhitelistedException;
import org.graylog2.system.urlwhitelist.UrlWhitelistNotificationService;
import org.graylog2.system.urlwhitelist.UrlWhitelistService;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Strings.isNullOrEmpty;

public class DSVHTTPDataAdapter extends LookupDataAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(DSVHTTPDataAdapter.class);

    public static final String NAME = "dsvhttp";

    private final DSVHTTPDataAdapter.Config config;
    private final HTTPFileRetriever httpFileRetriever;
    private final AtomicReference<Map<String, String>> lookupRef = new AtomicReference<>(Collections.emptyMap());
    private final DSVParser dsvParser;
    private final UrlWhitelistService whitelistService;
    private final UrlWhitelistNotificationService urlWhitelistNotificationService;

    @Inject
    public DSVHTTPDataAdapter(@Assisted("id") String id, @Assisted("name") String name,
            @Assisted LookupDataAdapterConfiguration config, MetricRegistry metricRegistry,
            HTTPFileRetriever httpFileRetriever, UrlWhitelistService whitelistService,
            UrlWhitelistNotificationService urlWhitelistNotificationService) {
        super(id, name, config, metricRegistry);
        this.config = (DSVHTTPDataAdapter.Config) config;
        this.httpFileRetriever = httpFileRetriever;
        this.whitelistService = whitelistService;
        this.urlWhitelistNotificationService = urlWhitelistNotificationService;
        this.dsvParser = new DSVParser(
                this.config.ignorechar(),
                this.config.lineSeparator(),
                this.config.separator(),
                this.config.quotechar(),
                this.config.isCheckPresenceOnly(),
                this.config.isCaseInsensitiveLookup(),
                this.config.keyColumn(),
                this.config.valueColumn()
        );
    }

    @Override
    public void doStart() throws Exception {
        LOG.debug("Starting HTTP DSV data adapter for URL: {}", config.url());
        if (isNullOrEmpty(config.url())) {
            throw new IllegalStateException("File path needs to be set");
        }
        if (config.refreshInterval() < 1) {
            throw new IllegalStateException("Check interval setting cannot be smaller than 1");
        }
        if (!whitelistService.isWhitelisted(config.url())) {
            publishSystemNotificationForWhitelistFailure();
            throw UrlNotWhitelistedException.forUrl(config.url());
        }

        final Optional<String> response = httpFileRetriever.fetchFileIfNotModified(config.url());

        response.ifPresent(body -> lookupRef.set(dsvParser.parse(body)));
    }

    @Override
    public Duration refreshInterval() {
        return Duration.standardSeconds(Ints.saturatedCast(config.refreshInterval()));
    }

    @Override
    protected void doRefresh(LookupCachePurge cachePurge) throws Exception {
        if (!whitelistService.isWhitelisted(config.url())) {
            setError(UrlNotWhitelistedException.forUrl(config.url()));
            publishSystemNotificationForWhitelistFailure();
            return;
        }

        final boolean urlWasNotWhitelisted =
                getError().filter(UrlNotWhitelistedException.class::isInstance).isPresent();

        try {
            // reload file if the url was blacklisted so that any errors with the file will surface again
            final Optional<String> response = urlWasNotWhitelisted ?
                    this.httpFileRetriever.fetchFile(config.url()) :
                    this.httpFileRetriever.fetchFileIfNotModified(config.url());

            response.ifPresent(body -> {
                LOG.debug("DSV file {} has changed, updating data", config.url());
                lookupRef.set(dsvParser.parse(body));
                cachePurge.purgeAll();
                clearError();
            });
        } catch (Exception e) {
            LOG.error("Couldn't check data adapter <{}> DSV file {} for updates: {} {}", name(), config.url(), e.getClass().getCanonicalName(), e.getMessage());
            setError(e);
        }
    }

    @Override
    public void doStop() throws Exception {
        LOG.debug("Stopping HTTP DSV data adapter for url: {}", config.url());
    }

    @Override
    public LookupResult doGet(Object key) {
        final String stringKey = config.isCaseInsensitiveLookup() ? String.valueOf(key).toLowerCase(Locale.ENGLISH) : String.valueOf(key);

        if (config.isCheckPresenceOnly()) {
            return LookupResult.single(lookupRef.get().containsKey(stringKey));
        }

        final String value = lookupRef.get().get(stringKey);

        if (value == null) {
            return LookupResult.empty();
        }

        return LookupResult.single(value);
    }

    @Override
    public void set(Object key, Object value) {
        throw new UnsupportedOperationException();
    }

    public interface Factory extends LookupDataAdapter.Factory<DSVHTTPDataAdapter> {
        @Override
        DSVHTTPDataAdapter create(@Assisted("id") String id,
                                  @Assisted("name") String name,
                                  LookupDataAdapterConfiguration configuration);

        @Override
        DSVHTTPDataAdapter.Descriptor getDescriptor();
    }

    public static class Descriptor extends LookupDataAdapter.Descriptor<DSVHTTPDataAdapter.Config> {
        public Descriptor() {
            super(NAME, DSVHTTPDataAdapter.Config.class);
        }

        @Override
        public DSVHTTPDataAdapter.Config defaultConfiguration() {
            return DSVHTTPDataAdapter.Config.builder()
                    .type(NAME)
                    .url("https://example.org/table.csv")
                    .separator(",")
                    .lineSeparator("\n")
                    .quotechar("\"")
                    .ignorechar("#")
                    .keyColumn(0)
                    .valueColumn(1)
                    .refreshInterval(60)
                    .caseInsensitiveLookup(false)
                    .checkPresenceOnly(false)
                    .build();
        }
    }

    @AutoValue
    @WithBeanGetter
    @JsonAutoDetect
    @JsonDeserialize(builder = AutoValue_DSVHTTPDataAdapter_Config.Builder.class)
    @JsonTypeName(NAME)
    public static abstract class Config implements LookupDataAdapterConfiguration {

        @Override
        @JsonProperty(TYPE_FIELD)
        public abstract String type();

        @JsonProperty("url")
        @NotEmpty
        public abstract String url();

        // Using String here instead of char to allow deserialization of a longer (invalid) string to get proper
        // validation error messages
        @JsonProperty("separator")
        @Size(min = 1, max = 1)
        @NotEmpty
        public abstract String separator();

        @JsonProperty("line_separator")
        @Size(min = 1, max = 1)
        @NotEmpty
        public abstract String lineSeparator();

        // Using String here instead of char to allow deserialization of a longer (invalid) string to get proper
        // validation error messages
        @JsonProperty("quotechar")
        @Size(min = 1, max = 1)
        @NotEmpty
        public abstract String quotechar();

        @JsonProperty("ignorechar")
        @Size(min = 1)
        @NotEmpty
        public abstract String ignorechar();

        @JsonProperty("key_column")
        @NotEmpty
        public abstract Integer keyColumn();

        @JsonProperty("check_presence_only")
        public abstract Optional<Boolean> checkPresenceOnly();

        @JsonProperty("value_column")
        @NotEmpty
        public abstract Optional<Integer> valueColumn();

        @JsonProperty("refresh_interval")
        @Min(1)
        public abstract long refreshInterval();

        @JsonProperty("case_insensitive_lookup")
        public abstract Optional<Boolean> caseInsensitiveLookup();

        public boolean isCaseInsensitiveLookup() {
            return caseInsensitiveLookup().orElse(false);
        }

        public boolean isCheckPresenceOnly() {
            return checkPresenceOnly().orElse(false);
        }

        public static DSVHTTPDataAdapter.Config.Builder builder() {
            return new AutoValue_DSVHTTPDataAdapter_Config.Builder();
        }

        @Override
        public Optional<Multimap<String, String>> validate(LookupDataAdapterValidationContext validationContext) {
            final ArrayListMultimap<String, String> errors = ArrayListMultimap.create();

            if (HttpUrl.parse(url()) == null) {
                errors.put("url", "Unable to parse url: " + url());
            } else if (!validationContext.getUrlWhitelistService().isWhitelisted(url())) {
                errors.put("url", "URL <" + url() + "> is not whitelisted.");
            }

            return errors.isEmpty() ? Optional.empty() : Optional.of(errors);
        }

        @AutoValue.Builder
        public abstract static class Builder {
            @JsonProperty(TYPE_FIELD)
            public abstract DSVHTTPDataAdapter.Config.Builder type(String type);

            @JsonProperty("url")
            public abstract DSVHTTPDataAdapter.Config.Builder url(String url);

            @JsonProperty("separator")
            public abstract DSVHTTPDataAdapter.Config.Builder separator(String separator);

            @JsonProperty("line_separator")
            public abstract DSVHTTPDataAdapter.Config.Builder lineSeparator(String separator);

            @JsonProperty("quotechar")
            public abstract DSVHTTPDataAdapter.Config.Builder quotechar(String quotechar);

            @JsonProperty("ignorechar")
            public abstract DSVHTTPDataAdapter.Config.Builder ignorechar(String ignorechar);

            @JsonProperty("key_column")
            public abstract DSVHTTPDataAdapter.Config.Builder keyColumn(Integer keyColumn);

            @JsonProperty("value_column")
            public abstract DSVHTTPDataAdapter.Config.Builder valueColumn(Integer valueColumn);

            @JsonProperty("refresh_interval")
            public abstract DSVHTTPDataAdapter.Config.Builder refreshInterval(long refreshInterval);

            @JsonProperty("case_insensitive_lookup")
            public abstract DSVHTTPDataAdapter.Config.Builder caseInsensitiveLookup(Boolean caseInsensitiveLookup);

            @JsonProperty("check_presence_only")
            public abstract DSVHTTPDataAdapter.Config.Builder checkPresenceOnly(Boolean checkPresenceOnly);

            public abstract DSVHTTPDataAdapter.Config build();
        }
    }

    private void publishSystemNotificationForWhitelistFailure() {
        final String description =
                "A \"DSV File from HTTP\" lookup adapter is trying to access a URL which is not whitelisted. Please " +
                        "check your configuration. [adapter name: \"" + name() + "\", url: \"" + config.url() +"\"]";
        urlWhitelistNotificationService.publishWhitelistFailure(description);
    }
}
