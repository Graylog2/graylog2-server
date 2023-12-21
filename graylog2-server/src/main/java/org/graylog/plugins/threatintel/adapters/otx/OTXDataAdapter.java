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
package org.graylog.plugins.threatintel.adapters.otx;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.inject.assistedinject.Assisted;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.lookup.LookupCachePurge;
import org.graylog2.plugin.lookup.LookupDataAdapter;
import org.graylog2.plugin.lookup.LookupDataAdapterConfiguration;
import org.graylog2.plugin.lookup.LookupResult;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import jakarta.inject.Inject;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Strings.isNullOrEmpty;

public class OTXDataAdapter extends LookupDataAdapter {
    public static final String NAME = "otx-api";

    private static final Logger LOG = LoggerFactory.getLogger(OTXDataAdapter.class);
    private static final InetAddressValidator INET_ADDRESS_VALIDATOR = InetAddressValidator.getInstance();
    // Don't use the object mapper from ObjectMapperProvider to make sure we are not affected by changes in that one
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<Object, Object>> MAP_TYPE_REFERENCE = new TypeReference<>() {
    };

    private static final String OTX_INDICATOR_IPV4 = "IPv4";
    private static final String OTX_INDICATOR_IPV6 = "IPv6";
    // The IP indicator is not an official OTX indicator - if this is used we do IPv4/6 auto detection
    private static final String OTX_INDICATOR_IP_AUTO_DETECT = "IPAutoDetect";
    private static final ImmutableSet<String> OTX_IP_INDICATORS = ImmutableSet.of(OTX_INDICATOR_IPV4, OTX_INDICATOR_IPV6);

    private static final String OTX_SECTION = "general";
    private static final ImmutableSet<String> OTX_INDICATORS = ImmutableSet.<String>builder()
            .add(OTX_INDICATOR_IP_AUTO_DETECT)
            .add(OTX_INDICATOR_IPV4)
            .add(OTX_INDICATOR_IPV6)
            .add("domain")
            .add("hostname")
            .add("file")
            .add("url")
            .add("cve")
            .add("nids")
            .add("correlation-rule")
            .build();

    private final Config config;
    private final OkHttpClient httpClient;
    private final Timer httpRequestTimer;
    private final Meter httpRequestErrors;
    private Headers httpHeaders;
    private HttpUrl parsedApiUrl;

    @Inject
    protected OTXDataAdapter(@Assisted("id") String id,
                             @Assisted("name") String name,
                             @Assisted LookupDataAdapterConfiguration config,
                             OkHttpClient httpClient,
                             MetricRegistry metricRegistry) {
        super(id, name, config, metricRegistry);

        this.config = (Config) config;
        this.httpClient = httpClient.newBuilder() // Copy HTTP client to be able to modify it
                .connectTimeout(this.config.httpConnectTimeout(), TimeUnit.MILLISECONDS)
                .writeTimeout(this.config.httpWriteTimeout(), TimeUnit.MILLISECONDS)
                .readTimeout(this.config.httpReadTimeout(), TimeUnit.MILLISECONDS)
                .build();

        this.httpRequestTimer = metricRegistry.timer(MetricRegistry.name(getClass(), "httpRequestTime"));
        this.httpRequestErrors = metricRegistry.meter(MetricRegistry.name(getClass(), "httpRequestErrors"));
    }

    @Override
    protected void doStart() throws Exception {
        final Headers.Builder builder = new Headers.Builder();

        final String apiKey = config.apiKey();
        if (isNullOrEmpty(apiKey)) {
            LOG.warn("OTX API key is missing. Make sure to add the key to allow higher request limits.");
        } else {
            builder.add("X-OTX-API-KEY", apiKey);
        }
        if (isNullOrEmpty(config.indicator())) {
            throw new IllegalArgumentException("OTX indicator is missing");
        }
        if (!OTX_INDICATORS.contains(config.indicator())) {
            throw new IllegalArgumentException("Invalid OTX indicator value - allowed: " + String.join(", ", OTX_INDICATORS));
        }
        if (isNullOrEmpty(config.httpUserAgent())) {
            throw new IllegalArgumentException("HTTP user-agent is missing");
        }

        if (isNullOrEmpty(config.apiUrl())) {
            throw new IllegalArgumentException("OTX API URL is missing");
        }
        final HttpUrl parsedUrl = HttpUrl.parse(config.apiUrl());
        if (parsedUrl != null) {
            this.parsedApiUrl = parsedUrl;
        } else {
            throw new IllegalArgumentException("OTX API URL is not valid");
        }

        this.httpHeaders = builder
                .add(HttpHeaders.USER_AGENT, config.httpUserAgent())
                .add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
                .build();
    }

    @Override
    protected void doStop() throws Exception {
        // Not needed
    }

    @Override
    public Duration refreshInterval() {
        return Duration.ZERO;
    }

    @Override
    protected void doRefresh(LookupCachePurge cachePurge) throws Exception {
        // Not needed
    }

    @Override
    protected LookupResult doGet(Object keyObject) {
        final String key = String.valueOf(keyObject);
        String otxIndicator = config.indicator();

        if (OTX_INDICATOR_IP_AUTO_DETECT.equals(otxIndicator)) {
            // If the indicator is IPAutoDetect, we try to detect the IP address type. If we cannot detect the IP
            // address type, it is not a valid IP address and we just return an empty result to avoid an unnecessary
            // HTTP request against the OTX API.
            final Optional<String> ipType = detectIpType(key);
            if (ipType.isPresent()) {
                otxIndicator = ipType.get();
            } else {
                LOG.warn("Unable to auto-detect IP address type for key <{}>", key);
                return LookupResult.empty();
            }
        }

        if (OTX_IP_INDICATORS.contains(otxIndicator) && isPrivateIPAddress(key)) {
            LOG.debug("OTX API does not accept private IP address <{}>. Skipping lookup to avoid OTX API request.", key);
            return LookupResult.empty();
        }

        final HttpUrl url = new HttpUrl.Builder()
                .scheme(parsedApiUrl.scheme())
                .host(parsedApiUrl.host())
                .port(parsedApiUrl.port())
                .addPathSegments("/api/v1/indicators")
                .addPathSegment(otxIndicator)
                .addPathSegment(String.valueOf(key))
                .addPathSegment(OTX_SECTION)
                .build();

        final Request request = new Request.Builder()
                .get()
                .url(url)
                .headers(httpHeaders)
                .build();

        final Timer.Context time = httpRequestTimer.time();
        try (final Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                LOG.warn("OTX {} request for key <{}> failed: {}", otxIndicator, key, response);
                httpRequestErrors.mark();
                return LookupResult.withError(
                        String.format(Locale.ENGLISH, "OTX %s request for key <%s> failed: %s", otxIndicator, key, response.code()));
            }

            return parseResponse(response.body());
        } catch (IOException e) {
            LOG.error("OTX {} request error for key <{}>", otxIndicator, key, e);
            httpRequestErrors.mark();
            return LookupResult.empty();
        } finally {
            time.stop();
        }
    }

    @VisibleForTesting
    @SuppressWarnings("WeakerAccess")
    LookupResult parseResponse(@Nullable ResponseBody body) {
        if (body != null) {
            try {
                final JsonNode json = OBJECT_MAPPER.readTree(body.string());

                return LookupResult.withoutTTL()
                        .single(json.path("pulse_info").path("count").asLong(0))
                        .multiValue(OBJECT_MAPPER.convertValue(json, MAP_TYPE_REFERENCE))
                        .build();
            } catch (IOException e) {
                LOG.warn("Couldn't parse OTX response as JSON", e);
            }
        }

        return LookupResult.empty();
    }

    @VisibleForTesting
    boolean isPrivateIPAddress(String ip) {
        try {
            final InetAddress inetAddress = InetAddress.getByName(ip);
            return inetAddress.isSiteLocalAddress() || inetAddress.isLoopbackAddress() || inetAddress.isAnyLocalAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }

    private Optional<String> detectIpType(String ip) {
        if (INET_ADDRESS_VALIDATOR.isValidInet4Address(ip)) {
            return Optional.of(OTX_INDICATOR_IPV4);
        } else if (INET_ADDRESS_VALIDATOR.isValidInet6Address(ip)) {
            return Optional.of(OTX_INDICATOR_IPV6);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void set(Object key, Object value) {
        // Not supported
    }

    public interface Factory extends LookupDataAdapter.Factory<OTXDataAdapter> {
        @Override
        OTXDataAdapter create(@Assisted("id") String id,
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
                    .indicator(OTX_INDICATOR_IP_AUTO_DETECT)
                    .apiUrl("https://otx.alienvault.com")
                    .httpUserAgent("Graylog Threat Intelligence Plugin - https://github.com/Graylog2/graylog-plugin-threatintel")
                    .httpConnectTimeout(10000)
                    .httpWriteTimeout(10000)
                    .httpReadTimeout(60000)
                    .build();
        }
    }

    @AutoValue
    @WithBeanGetter
    @JsonAutoDetect
    @JsonDeserialize(builder = Config.Builder.class)
    @JsonTypeName(NAME)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public abstract static class Config implements LookupDataAdapterConfiguration {
        @JsonProperty("indicator")
        @NotEmpty
        public abstract String indicator();

        @JsonProperty("api_key")
        @Nullable
        public abstract String apiKey();

        @JsonProperty("api_url")
        @NotEmpty
        public abstract String apiUrl();

        @JsonProperty("http_user_agent")
        @NotEmpty
        public abstract String httpUserAgent();

        @JsonProperty("http_connect_timeout")
        @Min(1)
        public abstract long httpConnectTimeout();

        @JsonProperty("http_write_timeout")
        @Min(1)
        public abstract long httpWriteTimeout();

        @JsonProperty("http_read_timeout")
        @Min(1)
        public abstract long httpReadTimeout();

        public static Builder builder() {
            return new AutoValue_OTXDataAdapter_Config.Builder();
        }

        public abstract Builder toBuilder();

        @Override
        public Optional<Multimap<String, String>> validate() {
            final ArrayListMultimap<String, String> errors = ArrayListMultimap.create();

            if (!OTX_INDICATORS.contains(indicator())) {
                errors.put("indicator", "Invalid value - allowed: " + String.join(", ", OTX_INDICATORS));
            }
            if (HttpUrl.parse(apiUrl()) == null) {
                errors.put("api_url", "Invalid URL");
            }
            if (httpConnectTimeout() < 1) {
                errors.put("http_connect_timeout", "Value cannot be smaller than 1");
            }
            if (httpWriteTimeout() < 1) {
                errors.put("http_write_timeout", "Value cannot be smaller than 1");
            }
            if (httpReadTimeout() < 1) {
                errors.put("http_read_timeout", "Value cannot be smaller than 1");
            }

            return errors.isEmpty() ? Optional.empty() : Optional.of(errors);
        }

        @AutoValue.Builder
        public abstract static class Builder {
            @JsonCreator
            public static Builder create() {
                return Config.builder()
                        .httpConnectTimeout(10000)
                        .httpWriteTimeout(10000)
                        .httpReadTimeout(60000);
            }

            @JsonProperty(TYPE_FIELD)
            public abstract Builder type(String type);

            @JsonProperty("indicator")
            public abstract Builder indicator(String indicator);

            @JsonProperty("api_key")
            public abstract Builder apiKey(String apiKey);

            @JsonProperty("api_url")
            public abstract Builder apiUrl(String apiUrl);

            @JsonProperty("http_user_agent")
            public abstract Builder httpUserAgent(String httpUserAgent);

            @JsonProperty("http_connect_timeout")
            public abstract Builder httpConnectTimeout(long httpConnectTimeout);

            @JsonProperty("http_write_timeout")
            public abstract Builder httpWriteTimeout(long httpWriteTimeout);

            @JsonProperty("http_read_timeout")
            public abstract Builder httpReadTimeout(long httpReadTimeout);

            public abstract Config build();
        }
    }
}
