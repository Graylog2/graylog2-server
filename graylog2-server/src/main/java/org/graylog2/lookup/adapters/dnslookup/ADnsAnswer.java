/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
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