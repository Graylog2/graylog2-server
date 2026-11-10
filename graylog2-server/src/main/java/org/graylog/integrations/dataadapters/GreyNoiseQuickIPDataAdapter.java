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
package org.graylog.integrations.dataadapters;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.inject.assistedinject.Assisted;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotEmpty;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.graylog.plugins.threatintel.tools.AdapterDisabledException;
import org.graylog2.plugin.lookup.LookupCachePurge;
import org.graylog2.plugin.lookup.LookupDataAdapter;
import org.graylog2.plugin.lookup.LookupDataAdapterConfiguration;
import org.graylog2.plugin.lookup.LookupResult;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.utilities.ReservedIpChecker;
import org.graylog2.web.customization.CustomizationConfig;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class GreyNoiseQuickIPDataAdapter extends LookupDataAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(GreyNoiseQuickIPDataAdapter.class);
    public static final String NAME = "GreyNoise";
    // v3 unified IP Lookup endpoint; the quick variant is selected with the "quick=true" query parameter.
    static final String GREYNOISE_IPQC_ENDPOINT = "https://api.greynoise.io/v3/ip/";

    private final EncryptedValueService encryptedValueService;
    private final Config config;
    private final OkHttpClient okHttpClient;
    private final CustomizationConfig customizationConfig;
    private final ObjectMapper objectMapper;

    private static final AtomicBoolean VALID_GREYNOISE_LICENSE = new AtomicBoolean(false);

    @Inject
    public GreyNoiseQuickIPDataAdapter(@Assisted("id") String id,
                                       @Assisted("name") String name,
                                       @Assisted LookupDataAdapterConfiguration config,
                                       MetricRegistry metricRegistry,
                                       EncryptedValueService encryptedValueService,
                                       OkHttpClient okHttpClient,
                                       CustomizationConfig customizationConfig,
                                       ObjectMapper objectMapper) {
        super(id, name, config, metricRegistry);
        this.config = (Config) config;
        this.encryptedValueService = encryptedValueService;
        this.okHttpClient = okHttpClient;
        this.customizationConfig = customizationConfig;
        this.objectMapper = objectMapper;
    }

    @Override
    public void doStart() throws Exception {
        if (!isValidSubscription(encryptedValueService.decrypt(config.apiToken()))) {
            VALID_GREYNOISE_LICENSE.set(false);
            throw new AdapterDisabledException("Cannot start Data Adapter without a GreyNoise Enterprise subscription. Check API key and restart Data Adapter.");
        } else {
            VALID_GREYNOISE_LICENSE.set(true);
        }
    }

    @Override
    public void doStop() throws Exception {

    }

    @Override
    public Duration refreshInterval() {
        return Duration.ZERO;
    }

    @Override
    protected void doRefresh(LookupCachePurge cachePurge) throws Exception {

    }

    @Override
    protected LookupResult doGet(Object keyObject) {
        if (!VALID_GREYNOISE_LICENSE.get()) {
            return LookupResult.withError("Cannot perform lookup without a GreyNoise Enterprise subscription."
                    + " Check API key and restart Data Adapter.");
        }

        String ip = keyObject.toString();
        // if the IP address is a valid IPv4 address, check to see if it is a reserved IP.
        // If it is, skip lookup and return empty. If it is not, proceed with lookup.
        if (InetAddressValidator.getInstance().isValidInet4Address(ip)) {
            if (ReservedIpChecker.getInstance().isReservedIpAddress(ip)) {
                LOG.info("'{}' is an unsupported reserved address. Skipping lookup.", ip);
                return LookupResult.empty();
            }
        } else {
            // if the IP address is not a valid IPv4 address, check to see if it is IPv6.
            // If it is not IPv6, skip lookup and return error. If it is, skip lookup and return empty.
            if (InetAddressValidator.getInstance().isValidInet6Address(ip)) {
                LOG.info("'{}' is an unsupported IPv6 address. Skipping lookup.", ip);
                return LookupResult.empty();
            } else {
                LOG.error("'{}' is not a valid IPv4 Address", ip);
                return LookupResult.withError();
            }
        }
        Request request = new Request.Builder()
                .url(GREYNOISE_IPQC_ENDPOINT + ip + "?quick=true")
                .method("GET", null)
                .addHeader("Accept", "application/json")
                .addHeader("key", Objects.requireNonNull(encryptedValueService.decrypt(config.apiToken())))
                .addHeader("User-Agent", customizationConfig.productName())
                .build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            return parseResponse(response);
        } catch (Exception e) {
            LOG.error("An error occurred while retrieving lookup result [{}]", e.toString());
            return LookupResult.withError();
        }
    }

    @VisibleForTesting
    LookupResult parseResponse(Response response) {
        if (response.isSuccessful()) {
            Map<Object, Object> map = Maps.newHashMap();

            try {
                JsonNode root = objectMapper.readTree(response.body().string());
                if (root.hasNonNull("ip")) {
                    map.put("ip", root.get("ip").asText());
                }

                // v3 nests the scanner data; "found" is the v3 equivalent of the v2 top-level "noise" flag.
                JsonNode isi = root.path("internet_scanner_intelligence");
                map.put("noise", isi.path("found").asBoolean(false));
                if (isi.hasNonNull("classification")) {
                    map.put("classification", isi.get("classification").asText());
                }

                // v3 nests the RIOT data under business_service_intelligence; "found" replaces the v2 "riot" flag.
                JsonNode bsi = root.path("business_service_intelligence");
                map.put("riot", bsi.path("found").asBoolean(false));
                final String trustLevel = bsi.path("trust_level").asText("");
                if (!trustLevel.isEmpty()) {
                    map.put("trust_level", trustLevel);
                }
            } catch (IOException e) {
                LOG.error("An error occurred while parsing Lookup result [{}]", e.toString());
            }
            return LookupResult.withoutTTL().multiValue(map).build();
        } else {
            return LookupResult.empty();
        }
    }

    @Override
    public void set(Object key, Object value) {
    }

    // Check if the provided API token is accepted by GreyNoise. The free/community tier has been retired, so any
    // authenticated request (HTTP 200) corresponds to a valid subscription, while an invalid or missing key returns
    // HTTP 401.
    private boolean isValidSubscription(String apiKey) {
        Request request = new Request.Builder()
                .url("https://api.greynoise.io/ping")
                .method("GET", null)
                .addHeader("Accept", "application/json")
                .addHeader("key", apiKey)
                .addHeader("User-Agent", "Graylog")
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            return response.isSuccessful();
        } catch (Exception e) {
            LOG.warn("An error occurred while retrieving subscription type.", e);
            return false;
        }
    }

    public interface Factory extends LookupDataAdapter.Factory<GreyNoiseQuickIPDataAdapter> {

        @Override
        GreyNoiseQuickIPDataAdapter create(@Assisted("id") String id,
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
                    .apiToken(EncryptedValue.createUnset())
                    .build();
        }

    }

    @AutoValue
    @JsonAutoDetect
    @JsonDeserialize(builder = GreyNoiseQuickIPDataAdapter.Config.Builder.class)
    @JsonTypeName(NAME)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static abstract class Config implements LookupDataAdapterConfiguration {

        @Override
        @JsonProperty(TYPE_FIELD)
        public abstract String type();

        @JsonProperty("api_token")
        @NotEmpty
        public abstract EncryptedValue apiToken();

        public abstract Builder toBuilder();

        public static Builder builder() {
            return new AutoValue_GreyNoiseQuickIPDataAdapter_Config.Builder();
        }

        @Override
        @JsonIgnore
        public LookupDataAdapterConfiguration prepareConfigUpdate(@Nonnull LookupDataAdapterConfiguration newConfig) {
            final Config newGreyNoiseConfig = (Config) newConfig;
            EncryptedValue newApiToken = newGreyNoiseConfig.apiToken();

            if (newApiToken.isKeepValue()) {
                newApiToken = apiToken();
            } else if (newApiToken.isDeleteValue()) {
                newApiToken = EncryptedValue.createUnset();
            }

            return newGreyNoiseConfig.toBuilder().apiToken(newApiToken).build();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            @JsonCreator
            public static Builder create() {
                return Config.builder();
            }

            @JsonProperty(TYPE_FIELD)
            public abstract Builder type(String type);

            @JsonProperty("api_token")
            public abstract Builder apiToken(EncryptedValue api_token);

            public abstract Config build();
        }
    }
}
