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
package org.graylog2.lookup.adapters.dnslookup;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Address (A/AAAA) DNS lookup response from {@link DnsClient}.
 */
@AutoValue
@WithBeanGetter
@JsonAutoDetect
@JsonDeserialize(builder = ADnsAnswer.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ADnsAnswer.FIELD_IP_ADDRESS, ADnsAnswer.FIELD_IP_VERSION, ADnsAnswer.FIELD_DNS_TTL})
public abstract class ADnsAnswer implements DnsAnswer {

    static final String FIELD_IP_ADDRESS = "ip_address";
    static final String FIELD_IP_VERSION = "ip_version";
    static final String FIELD_DNS_TTL = "dns_ttl";

    @JsonProperty(FIELD_IP_ADDRESS)
    public abstract String ipAddress();

    // Deliberately nullable, because only included when IPv4 and IPv6 results are supplied together
    // (see usages of DnsLookupType.A_AAAA)
    @Nullable
    @JsonProperty(FIELD_IP_VERSION)
    public abstract String ipVersion();

    @Override
    @JsonProperty(FIELD_DNS_TTL)
    public abstract long dnsTTL();

    public static Builder builder() {
        return new AutoValue_ADnsAnswer.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        @JsonCreator
        public static Builder createDefault() {
            return builder();
        }

        @JsonProperty(FIELD_IP_ADDRESS)
        public abstract ADnsAnswer.Builder ipAddress(String ipAddress);

        // Deliberately nullable, because only included when IPv4 and IPv6 results are supplied together
        // (see usages of DnsLookupType.A_AAAA)
        @Nullable
        @JsonProperty(FIELD_IP_VERSION)
        public abstract ADnsAnswer.Builder ipVersion(String ipVersion);

        @JsonProperty(FIELD_DNS_TTL)
        public abstract ADnsAnswer.Builder dnsTTL(long dnsTTL);

        abstract ADnsAnswer autoBuild();

        public ADnsAnswer build() {

            return autoBuild();
        }
    }

    public static List<String> convertToStringListValue(List<ADnsAnswer> aDnsAnswers) {
        return aDnsAnswers.stream().map(ADnsAnswer::ipAddress).collect(Collectors.toList());
    }
}