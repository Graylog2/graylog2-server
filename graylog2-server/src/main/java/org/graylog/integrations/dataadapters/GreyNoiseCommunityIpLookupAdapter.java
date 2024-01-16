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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.assistedinject.Assisted;
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
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import jakarta.validation.constraints.NotEmpty;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The GreyNoiseCommunityIpLookupAdapter class is deprecated as of <a href="https://github.com/Graylog2/graylog-plugin-integrations/pull/1340</a>.
 *
 * A {@link LookupDataAdapter} that uses the <a href="https://docs.greynoise.io/reference/get_v3-community-ip">GreyNoise Community API</a>
 * to perform IP lookups.
 *
 * <p>
 * The API response is a subset of the IP context returned by the full IP Lookup API.
 * </p>
 */
@Deprecated
public class GreyNoiseCommunityIpLookupAdapter extends LookupDataAdapter {

    public static final String ADAPTER_NAME = "GreyNoise Community IP Lookup";

    protected static final String GREYNOISE_COMMUNITY_ENDPOINT = "https://api.greynoise.io/v3/community";

    private static final Logger LOG = LoggerFactory.getLogger(GreyNoiseCommunityIpLookupAdapter.class);

    private static final List<Object> EXCLUDED_FIELDS = Collections.singletonList("message");

    private static final String USER_AGENT = "Graylog";
    private static final String ACCEPT_TYPE = "application/json";
    private static final String METHOD = "GET";

    private final EncryptedValueService encryptedValueService;
    private final Config config;
    private final OkHttpClient okHttpClient;

    @Inject
    public GreyNoiseCommunityIpLookupAdapter(@Assisted("id") String id,
                                             @Assisted("name") String name,
                                             @Assisted LookupDataAdapterConfiguration config,
                                             MetricRegistry metricRegistry,
                                             EncryptedValueService encryptedValueService,
                                             OkHttpClient okHttpClient) {
        super(id, name, config, metricRegistry);
        this.config = (GreyNoiseCommunityIpLookupAdapter.Config) config;
        this.encryptedValueService = encryptedValueService;
        this.okHttpClient = okHttpClient;
    }

    @Override
    protected void doStart() throws Exception {
        throw new AdapterDisabledException("The GreyNoise Community IP Lookup Data Adapter is no longer supported. This Data Adapter should be deleted.");
    }

    @Override
    protected void doStop() {
        //Not needed
    }

    @Override
    public Duration refreshInterval() {
        return Duration.ZERO;
    }

    @Override
    protected void doRefresh(LookupCachePurge cachePurge) {
        //Not needed
    }

    @Override
    public void set(Object key, Object value) {
        //Not needed
    }

    @Override
    protected LookupResult doGet(Object ipAddress) {
        return LookupResult.withError("GreyNoise Community IP Lookup Data Adapter is deprecated and lookups can no longer be performed.");
    }

    // This is the old doGet() method, left in case this functionality needs to be restored in the future.
    protected LookupResult doDoGet(Object ipAddress) {


        Optional<Request> request = createRequest(ipAddress);
        if (!request.isPresent()) {
            return LookupResult.withError();
        }

        LookupResult result;
        try (Response response = okHttpClient.newCall(request.get()).execute()) {
            result = parseResponse(response);

        } catch (IOException e) {
            LOG.error("an error occurred while retrieving GreyNoise IP data. {}", e.getMessage(), e);
            result = LookupResult.withError();
        }
        return result;
    }

    @VisibleForTesting
    Optional<Request> createRequest(Object ipAddress) {
        Optional<String> ipString = getValidIpString(ipAddress);

        if (!ipString.isPresent()) {
            return Optional.empty();
        }
        String apiToken = encryptedValueService.decrypt(config.apiToken());
        if (apiToken == null || apiToken.trim().isEmpty()) {
            String error = String.format(Locale.ROOT, "[%s] requires a non-null API Token", ADAPTER_NAME);
            throw new IllegalArgumentException(error);
        }

        Request request = new Request.Builder()
                .url(String.join("/", GREYNOISE_COMMUNITY_ENDPOINT, ipString.get()))
                .method(METHOD, null)
                .addHeader("Accept", ACCEPT_TYPE)
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("key", apiToken)
                .build();

        return Optional.of(request);
    }

    private Optional<String> getValidIpString(Object ipAddress) {
        final String ipString = ipAddress == null ? "" : ipAddress.toString();
        if (ipString.trim().isEmpty()) {
            String error = String.format(Locale.ROOT, "'%s' requires an IP address to perform Lookup", ADAPTER_NAME);
            throw new IllegalArgumentException(error);
        }

        String validIpAddress = null;
        final InetAddressValidator validator = InetAddressValidator.getInstance();
        if (validator.isValidInet6Address(ipString)) {
            LOG.warn("'{}' is an IPv6 Address.  '{}' does not support IPv6 Addresses", ipAddress, ADAPTER_NAME);
        } else if (!validator.isValidInet4Address(ipString)) {
            LOG.error("'{}' is not a valid IPv4 Address", ipString);
        } else if (ReservedIpChecker.getInstance().isReservedIpAddress(ipString)) {
            LOG.error("'{}' is a Reserved address", ipAddress);
        } else {
            validIpAddress = ipString;
        }

        return Optional.ofNullable(validIpAddress);
    }

    @VisibleForTesting
    static LookupResult parseResponse(Response response) {

        final LookupResult result;
        if (response.isSuccessful()) {
            result = createSuccessfulResult(response);
        } else {
            result = createUnsuccessfulResult(response);
        }

        return result;
    }

    private static LookupResult createUnsuccessfulResult(Response response) {

        Map<Object, Object> allValues = getResponseValueMap(response);

        //a 404 indicates the input IP does not exist
        //but the call as such was successful and thus no actual error has occurred.
        boolean hasError = response.code() != 404;

        return LookupResult.withoutTTL()
                .multiValue(allValues)
                .hasError(hasError)
                .build();
    }

    private static LookupResult createSuccessfulResult(Response response) {

        Map<Object, Object> allValues = getResponseValueMap(response);

        return LookupResult.withoutTTL()
                .multiValue(allValues)
                .build();
    }

    /**
     * Load the {@link Response#body()} fields into a {@link Map}.
     *
     * @param response response
     * @return map of fields
     */
    private static Map<Object, Object> getResponseValueMap(Response response) {
        Map<Object, Object> values;
        try {
            if (response.body() == null) {
                values = Collections.emptyMap();
            } else {
                ObjectMapper mapper = new ObjectMapper();
                TypeReference<Map<Object, Object>> ref = new TypeReference<Map<Object, Object>>() {};
                values = mapper.readValue(response.body().byteStream(), ref);
            }

        } catch (IOException e) {
            LOG.error("An error occurred while parsing parsing Lookup result. {}", e.getMessage(), e);
            values = Collections.emptyMap();
        }

        return values.entrySet()
                .stream()
                .filter(e -> !EXCLUDED_FIELDS.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public interface Factory extends LookupDataAdapter.Factory<GreyNoiseCommunityIpLookupAdapter> {

        @Override
        GreyNoiseCommunityIpLookupAdapter create(@Assisted("id") String id,
                                                 @Assisted("name") String name,
                                                 LookupDataAdapterConfiguration configuration);

        @Override
        Descriptor getDescriptor();

    }

    public static class Descriptor extends LookupDataAdapter.Descriptor<GreyNoiseCommunityIpLookupAdapter.Config> {

        public Descriptor() {
            super(ADAPTER_NAME, GreyNoiseCommunityIpLookupAdapter.Config.class);
        }

        @Override
        public GreyNoiseCommunityIpLookupAdapter.Config defaultConfiguration() {
            return GreyNoiseCommunityIpLookupAdapter.Config.builder()
                    .type(ADAPTER_NAME)
                    .apiToken(EncryptedValue.createUnset())
                    .build();
        }
    }

    @AutoValue
    @JsonAutoDetect
    @JsonDeserialize(builder = GreyNoiseCommunityIpLookupAdapter.Config.Builder.class)
    @JsonTypeName(ADAPTER_NAME)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public abstract static class Config implements LookupDataAdapterConfiguration {

        @JsonProperty("api_token")
        @NotEmpty
        public abstract EncryptedValue apiToken();

        public static GreyNoiseCommunityIpLookupAdapter.Config.Builder builder() {
            return new AutoValue_GreyNoiseCommunityIpLookupAdapter_Config.Builder();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            @JsonCreator
            public static Builder create() {
                return builder();
            }

            @JsonProperty(TYPE_FIELD)
            public abstract Builder type(String type);

            @JsonProperty("api_token")
            public abstract Builder apiToken(EncryptedValue apiToken);

            public abstract GreyNoiseCommunityIpLookupAdapter.Config build();
        }
    }
}
