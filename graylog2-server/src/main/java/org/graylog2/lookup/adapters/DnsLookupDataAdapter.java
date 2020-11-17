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


import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.inject.assistedinject.Assisted;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.lookup.adapters.dnslookup.ADnsAnswer;
import org.graylog2.lookup.adapters.dnslookup.DnsAnswer;
import org.graylog2.lookup.adapters.dnslookup.DnsClient;
import org.graylog2.lookup.adapters.dnslookup.DnsLookupType;
import org.graylog2.lookup.adapters.dnslookup.PtrDnsAnswer;
import org.graylog2.lookup.adapters.dnslookup.TxtDnsAnswer;
import org.graylog2.lookup.dto.DataAdapterDto;
import org.graylog2.plugin.lookup.LookupCachePurge;
import org.graylog2.plugin.lookup.LookupDataAdapter;
import org.graylog2.plugin.lookup.LookupDataAdapterConfiguration;
import org.graylog2.plugin.lookup.LookupResult;
import org.graylog2.shared.utilities.ExceptionUtils;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class DnsLookupDataAdapter extends LookupDataAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(DnsLookupDataAdapter.class);

    public static final String NAME = "dnslookup";

    private static final Duration REFRESH_INTERVAL_DURATION = Duration.ZERO;
    private static final String A_RECORD_LABEL = "A";
    private static final String AAAA_RECORD_LABEL = "AAAA";
    private static final String ERROR_COUNTER = "errors";
    private static final String RESULTS_FIELD = "results";
    private static final String RAW_RESULTS_FIELD = "raw_results";
    private static final String TIMER_RESOLVE_DOMAIN_NAME = "resolveDomainNameTime";
    private static final String TIMER_REVERSE_LOOKUP = "reverseLookupTime";
    private static final String TIMER_TEXT_LOOKUP = "textLookupTime";
    private DnsClient dnsClient;
    private final Config config;

    private final Counter errorCounter;

    // Timers exist for all request types, so that each can be troubleshot individually.
    private final Timer resolveDomainNameTimer;
    private final Timer reverseLookupTimer;
    private final Timer textLookupTimer;

    @Inject
    public DnsLookupDataAdapter(@Assisted("dto") DataAdapterDto dto,
                                MetricRegistry metricRegistry) {
        super(dto, metricRegistry);
        this.config = (Config) dto.config();
        this.errorCounter = metricRegistry.counter(MetricRegistry.name(getClass(), dto.id(), ERROR_COUNTER));
        this.resolveDomainNameTimer = metricRegistry.timer(MetricRegistry.name(getClass(), dto.id(), TIMER_RESOLVE_DOMAIN_NAME));
        this.reverseLookupTimer = metricRegistry.timer(MetricRegistry.name(getClass(), dto.id(), TIMER_REVERSE_LOOKUP));
        this.textLookupTimer = metricRegistry.timer(MetricRegistry.name(getClass(), dto.id(), TIMER_TEXT_LOOKUP));
    }

    @Override
    protected void doStart() {

        dnsClient = new DnsClient(config.requestTimeout());
        dnsClient.start(config.serverIps());
    }

    @Override
    protected void doStop() {

        dnsClient.stop();
    }

    /**
     * Not needed for the DNS Lookup adaptor.
     */
    @Override
    public Duration refreshInterval() {
        return REFRESH_INTERVAL_DURATION;
    }

    /**
     * Not needed for the DNS Lookup adaptor.
     */
    @Override
    protected void doRefresh(LookupCachePurge cachePurge) {

    }

    @Override
    protected LookupResult doGet(Object key) {

        final String trimmedKey = StringUtils.trimToNull(key.toString());
        if (trimmedKey == null) {
            LOG.debug("A blank key was supplied");
            return getEmptyResult();
        }

        LOG.debug("Beginning [{}] DNS resolution for key [{}]", config.lookupType(), trimmedKey);

        LookupResult lookupResult;
        switch (config.lookupType()) {
            case A:
                try (final Timer.Context ignored = resolveDomainNameTimer.time()) {
                    lookupResult = resolveIPv4AddressForHostname(trimmedKey);
                }
                break;
            case AAAA: {
                try (final Timer.Context ignored = resolveDomainNameTimer.time()) {
                    lookupResult = resolveIPv6AddressForHostname(trimmedKey);
                }
                break;
            }
            case A_AAAA: {
                try (final Timer.Context ignored = resolveDomainNameTimer.time()) {
                    lookupResult = resolveAllAddressesForHostname(trimmedKey);
                }
                break;
            }
            case PTR: {
                try (final Timer.Context ignored = reverseLookupTimer.time()) {
                    lookupResult = performReverseLookup(trimmedKey);
                }
                break;
            }
            case TXT: {
                try (final Timer.Context ignored = textLookupTimer.time()) {
                    lookupResult = performTextLookup(trimmedKey);
                }
                break;
            }
            default:
                throw new IllegalArgumentException(String.format(Locale.ENGLISH, "DnsLookupType [%s] is not supported", config.lookupType()));
        }

        LOG.debug("[{}] DNS resolution complete for key [{}]. Response [{}]", config.lookupType(), trimmedKey, lookupResult);

        return lookupResult;
    }

    /**
     * Provides both single and multiple addresses in LookupResult. This is because the purpose of a hostname
     * resolution request is to resolve to a single IP address (so that communication can be initiated with it).
     * We also resolve all addresses in case they are needed.
     */
    private LookupResult resolveIPv4AddressForHostname(Object key) {

        final List<ADnsAnswer> aDnsAnswers;
        try {
            aDnsAnswers = dnsClient.resolveIPv4AddressForHostname(key.toString(), false);
        } catch (UnknownHostException e) {
            return LookupResult.empty(); // UnknownHostException is a valid case when the DNS record does not exist. Do not log an error.
        } catch (Exception e) {
            LOG.error("Could not resolve [{}] records for hostname [{}]. Cause [{}]", A_RECORD_LABEL, key, ExceptionUtils.getRootCauseOrMessage(e));
            errorCounter.inc();
            return getEmptyResult();
        }

        if (CollectionUtils.isNotEmpty(aDnsAnswers)) {
            return buildLookupResult(aDnsAnswers);
        }

        LOG.debug("Could not resolve [{}] records for hostname [{}].", A_RECORD_LABEL, key);
        return getEmptyResult();
    }

    /**
     * Provides both single and multiple addresses in LookupResult. This is because the purpose of a hostname
     * resolution request is to resolve to a single IP address (so that communication can be initiated with it).
     * We also resolve all addresses in case they are needed.
     */
    private LookupResult resolveIPv6AddressForHostname(Object key) {

        final List<ADnsAnswer> aDnsAnswers;
        try {
            aDnsAnswers = dnsClient.resolveIPv6AddressForHostname(key.toString(), false);
        } catch (UnknownHostException e) {
            return getEmptyResult(); // UnknownHostException is a valid case when the DNS record does not exist. Do not log an error.
        } catch (Exception e) {
            LOG.error("Could not resolve [{}] records for hostname [{}]. Cause [{}]", AAAA_RECORD_LABEL, key, ExceptionUtils.getRootCauseOrMessage(e));
            errorCounter.inc();
            return getErrorResult();
        }

        if (CollectionUtils.isNotEmpty(aDnsAnswers)) {
            return buildLookupResult(aDnsAnswers);
        }

        LOG.debug("Could not resolve [{}] records for hostname [{}].", AAAA_RECORD_LABEL, key);
        return getEmptyResult();
    }

    private LookupResult buildLookupResult(List<ADnsAnswer> aDnsAnswers) {

        /* Provide both a single and multiValue addresses.
         * Always read the first entry and place in singleValue */
        final String singleValue = aDnsAnswers.get(0).ipAddress();
        LookupResult.Builder builder = LookupResult.builder()
                                                   .single(singleValue)
                                                   .multiValue(Collections.singletonMap(RESULTS_FIELD, aDnsAnswers))
                                                   .stringListValue(ADnsAnswer.convertToStringListValue(aDnsAnswers));

        assignMinimumTTL(aDnsAnswers, builder);

        return builder.build();
    }

    /**
     * Resolves all IPv4 and IPv6 addresses for the hostname. A single IP address will be returned in the singleValue
     * field (IPv4 address will be returned if present). All IPv4 and IPv6 addresses will be included in the multiValue
     * field.
     *
     * @param key a hostname
     */
    private LookupResult resolveAllAddressesForHostname(Object key) {

        try {
            List<ADnsAnswer> ip4Answers = new ArrayList<>();
            List<ADnsAnswer> ip6Answers = new ArrayList<>();

            // UnknownHostException is a valid case when the DNS record does not exist. Silently ignore and do not log an error.
            try {
                ip4Answers = dnsClient.resolveIPv4AddressForHostname(key.toString(), true); // Include IP version
            } catch (UnknownHostException e) {
            }

            try {
                ip6Answers = dnsClient.resolveIPv6AddressForHostname(key.toString(), true); // Include IP version
            } catch (UnknownHostException e) {
            }

            // Select answer for single value. Prefer use of IPv4 address. Only return IPv6 address if no IPv6 address found.
            final String singleValue;
            if (CollectionUtils.isNotEmpty(ip4Answers)) {
                singleValue = ip4Answers.get(0).ipAddress();
            } else if (CollectionUtils.isNotEmpty(ip6Answers)) {
                singleValue = ip6Answers.get(0).ipAddress();
            } else {
                LOG.debug("Could not resolve [A/AAAA] records hostname [{}].", key);
                return getEmptyResult();
            }

            final LookupResult.Builder builder = LookupResult.builder();
            if (StringUtils.isNotBlank(singleValue)) {
                builder.single(singleValue);
            }

            final List<ADnsAnswer> allAnswers = new ArrayList<>();
            allAnswers.addAll(ip4Answers);
            allAnswers.addAll(ip6Answers);

            if (CollectionUtils.isNotEmpty(allAnswers)) {
                builder.multiValue(Collections.singletonMap(RESULTS_FIELD, allAnswers)).stringListValue(ADnsAnswer.convertToStringListValue(allAnswers));
            }

            assignMinimumTTL(allAnswers, builder);

            return builder.build();
        } catch (Exception e) {
            LOG.error("Could not resolve [A/AAAA] records for hostname [{}]. Cause [{}]", key, ExceptionUtils.getRootCauseOrMessage(e));
            errorCounter.inc();
            return getErrorResult();
        }
    }

    private LookupResult performReverseLookup(Object key) {

        final PtrDnsAnswer dnsResponse;
        try {
            dnsResponse = dnsClient.reverseLookup(key.toString());
        } catch (Exception e) {
            LOG.error("Could not perform reverse DNS lookup for [{}]. Cause [{}]", key, ExceptionUtils.getRootCauseOrMessage(e));
            errorCounter.inc();
            return getErrorResult();
        }

        if (dnsResponse != null) {
            if (!Strings.isNullOrEmpty(dnsResponse.fullDomain())) {

                // Include answer in both single and multiValue fields.
                final Map<Object, Object> multiValueResults = new LinkedHashMap<>();
                multiValueResults.put(PtrDnsAnswer.FIELD_DOMAIN, dnsResponse.domain());
                multiValueResults.put(PtrDnsAnswer.FIELD_FULL_DOMAIN, dnsResponse.fullDomain());
                multiValueResults.put(PtrDnsAnswer.FIELD_DNS_TTL, dnsResponse.dnsTTL());

                final LookupResult.Builder builder = LookupResult.builder()
                        .single(dnsResponse.fullDomain())
                        .multiValue(multiValueResults)
                        .stringListValue(ImmutableList.of(dnsResponse.fullDomain()));

                if (config.hasOverrideTTL()) {
                    builder.cacheTTL(config.getCacheTTLOverrideMillis());
                } else {
                    builder.cacheTTL(dnsResponse.dnsTTL() * 1000);
                }

                return builder.build();
            }
        }

        LOG.debug("Could not perform reverse lookup on IP address [{}]. No PTR record was found.", key);
        return getEmptyResult();
    }

    private LookupResult performTextLookup(Object key) {

        /* Query all TXT records for hostname and provide them in the multiValue field as an array.
         * Do not attempt to attempt to choose a single value for the user (all are valid). */
        final List<TxtDnsAnswer> txtDnsAnswers;
        try {
            txtDnsAnswers = dnsClient.txtLookup(key.toString());
        } catch (Exception e) {
            LOG.error("Could not perform TXT DNS lookup for [{}]. Cause [{}]", key, ExceptionUtils.getRootCauseOrMessage(e));
            errorCounter.inc();
            return getErrorResult();
        }

        if (CollectionUtils.isNotEmpty(txtDnsAnswers)) {
            final LookupResult.Builder builder = LookupResult.builder();
            builder.multiValue(Collections.singletonMap(RAW_RESULTS_FIELD, txtDnsAnswers))
                    .stringListValue(TxtDnsAnswer.convertToStringListValue(txtDnsAnswers));
            assignMinimumTTL(txtDnsAnswers, builder);

            return builder.build();
        }

        LOG.debug("Could not perform Text lookup on IP address [{}]. No TXT records were found.", key);
        return getEmptyResult();
    }

    /**
     * Assigns the minimum TTL found in the supplied DnsAnswers. The minimum makes sense, because this is the least
     * amount of time that at least one of the records is valid for.
     */
    private void assignMinimumTTL(List<? extends DnsAnswer> dnsAnswers, LookupResult.Builder builder) {

        if (config.hasOverrideTTL()) {
            builder.cacheTTL(config.getCacheTTLOverrideMillis());
        } else {
            // Deduce minimum TTL on all TXT records. A TTL will always be returned by DNS server.
            builder.cacheTTL(dnsAnswers.stream()
                                       .map(DnsAnswer::dnsTTL)
                                       .min(Comparator.comparing(Long::valueOf)).get() * 1000);
        }
    }

    @Override
    public void set(Object key, Object value) {
        throw new UnsupportedOperationException();
    }

    public interface Factory extends LookupDataAdapter.Factory2<DnsLookupDataAdapter> {
        @Override
        DnsLookupDataAdapter create(@Assisted("dto") DataAdapterDto dto);

        @Override
        DnsLookupDataAdapter.Descriptor getDescriptor();
    }

    public static class Descriptor extends LookupDataAdapter.Descriptor<Config> {
        public Descriptor() {
            super(NAME, DnsLookupDataAdapter.Config.class);
        }

        @Override
        public DnsLookupDataAdapter.Config defaultConfiguration() {
            return DnsLookupDataAdapter.Config.builder()
                                              .type(NAME)
                                              .lookupType(Config.DEFAULT_LOOKUP_TYPE)
                                              .serverIps(Config.DEFAULT_SERVER_IP)
                                              .cacheTTLOverrideEnabled(Config.DEFAULT_CACHE_TTL_OVERRIDE)
                                              .requestTimeout(Config.DEFAULT_TIMEOUT_MILLIS)
                                              .build();
        }
    }

    @AutoValue
    @WithBeanGetter
    @JsonAutoDetect
    @JsonDeserialize(builder = DnsLookupDataAdapter.Config.Builder.class)
    @JsonTypeName(NAME)
    public static abstract class Config implements LookupDataAdapterConfiguration {

        // Fields
        private static final String FIELD_CACHE_TTL_OVERRIDE = "cache_ttl_override";
        private static final String FIELD_CACHE_TTL_OVERRIDE_ENABLED = "cache_ttl_override_enabled";
        private static final String FIELD_CACHE_TTL_OVERRIDE_UNIT = "cache_ttl_override_unit";
        private static final String FIELD_LOOKUP_TYPE = "lookup_type";
        private static final String FIELD_REQUEST_TIMEOUT = "request_timeout";
        private static final String FIELD_SERVER_IPS = "server_ips";

        // Default values
        private static final boolean DEFAULT_CACHE_TTL_OVERRIDE = false;
        private static final DnsLookupType DEFAULT_LOOKUP_TYPE = DnsLookupType.A;
        private static final int DEFAULT_TIMEOUT_MILLIS = 10000;
        private static final String DEFAULT_SERVER_IP = ""; // Intentionally blank

        @Override
        @JsonProperty(TYPE_FIELD)
        public abstract String type();

        @JsonProperty(FIELD_LOOKUP_TYPE)
        public abstract DnsLookupType lookupType();

        @JsonProperty(FIELD_SERVER_IPS)
        public abstract String serverIps();

        @JsonProperty(FIELD_REQUEST_TIMEOUT)
        public abstract int requestTimeout();

        @JsonProperty(FIELD_CACHE_TTL_OVERRIDE_ENABLED)
        public abstract boolean cacheTTLOverrideEnabled();

        @Nullable
        @JsonProperty(FIELD_CACHE_TTL_OVERRIDE)
        public abstract Long cacheTTLOverride();

        @Nullable
        @JsonProperty(FIELD_CACHE_TTL_OVERRIDE_UNIT)
        public abstract TimeUnit cacheTTLOverrideUnit();

        public static Builder builder() {
            return new AutoValue_DnsLookupDataAdapter_Config.Builder();
        }

        @Override
        public Optional<Multimap<String, String>> validate() {

            final ArrayListMultimap<String, String> errors = ArrayListMultimap.create();
            if (StringUtils.isNotBlank(serverIps()) && !DnsClient.allIpAddressesValid(serverIps())) {
                errors.put(FIELD_SERVER_IPS, "Invalid server IP address and/or port. " +
                                             "Please enter one or more comma-separated IPv4 addresses with optional ports (eg. 192.168.1.1:5353, 192.168.1.244).");
            }

            if (requestTimeout() < 1) {
                errors.put(FIELD_REQUEST_TIMEOUT, "Value cannot be smaller than 1");
            }

            return errors.isEmpty() ? Optional.empty() : Optional.of(errors);
        }

        private boolean hasOverrideTTL() {

            return cacheTTLOverride() != null && cacheTTLOverrideUnit() != null;
        }

        private Long getCacheTTLOverrideMillis() {

            // Valid default, because Cache DNS_TTL is always required.
            if (!cacheTTLOverrideEnabled() || cacheTTLOverride() == null || cacheTTLOverrideUnit() == null) {
                return Long.MAX_VALUE;
            }

            return cacheTTLOverrideUnit().toMillis(cacheTTLOverride());
        }

        @AutoValue.Builder
        public abstract static class Builder {

            @JsonCreator
            public static Builder create() {
                return Config.builder()
                             .serverIps(DEFAULT_SERVER_IP)
                             .lookupType(DnsLookupType.A)
                             .cacheTTLOverrideEnabled(DEFAULT_CACHE_TTL_OVERRIDE)
                             .requestTimeout(DEFAULT_TIMEOUT_MILLIS);
            }

            @JsonProperty(TYPE_FIELD)
            public abstract Builder type(String type);

            @JsonProperty(FIELD_LOOKUP_TYPE)
            public abstract Builder lookupType(DnsLookupType lookupType);

            @JsonProperty(FIELD_SERVER_IPS)
            public abstract Builder serverIps(String serverIp);

            @JsonProperty(FIELD_REQUEST_TIMEOUT)
            public abstract Builder requestTimeout(int requestTimeout);

            @JsonProperty(FIELD_CACHE_TTL_OVERRIDE_ENABLED)
            public abstract Builder cacheTTLOverrideEnabled(boolean cacheTTLOverride);

            @JsonProperty(FIELD_CACHE_TTL_OVERRIDE)
            public abstract Builder cacheTTLOverride(Long cacheTTLOverride);

            @JsonProperty(FIELD_CACHE_TTL_OVERRIDE_UNIT)
            public abstract Builder cacheTTLOverrideUnit(@Nullable TimeUnit cacheTTLOverrideUnit);

            abstract Config autoBuild();

            public Config build() {

                return autoBuild();
            }
        }
    }
}
