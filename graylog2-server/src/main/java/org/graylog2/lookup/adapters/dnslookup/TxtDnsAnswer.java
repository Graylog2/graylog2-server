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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Text (TXT) DNS lookup response from {@link DnsClient}.
 */
@AutoValue
@WithBeanGetter
@JsonAutoDetect
@JsonDeserialize(builder = TxtDnsAnswer.Builder.class)
@JsonPropertyOrder({TxtDnsAnswer.FIELD_VALUE, TxtDnsAnswer.FIELD_DNS_TTL})
public abstract class TxtDnsAnswer implements DnsAnswer {

    static final String FIELD_VALUE = "value";
    static final String FIELD_DNS_TTL = "dns_ttl";

    @JsonProperty(FIELD_VALUE)
    public abstract String value();

    @Override
    @JsonProperty(FIELD_DNS_TTL)
    public abstract long dnsTTL();

    public static Builder builder() {
        return new AutoValue_TxtDnsAnswer.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        @JsonCreator
        public static Builder createDefault() {
            return builder();
        }

        @JsonProperty(FIELD_VALUE)
        public abstract TxtDnsAnswer.Builder value(String value);

        @JsonProperty(FIELD_DNS_TTL)
        public abstract TxtDnsAnswer.Builder dnsTTL(long dnsTTL);

        abstract TxtDnsAnswer autoBuild();

        public TxtDnsAnswer build() {

            return autoBuild();
        }
    }

    public static List<String> convertToStringListValue(List<TxtDnsAnswer> txtDnsAnswer) {
        return txtDnsAnswer.stream().map(TxtDnsAnswer::value).collect(Collectors.toList());
    }
}