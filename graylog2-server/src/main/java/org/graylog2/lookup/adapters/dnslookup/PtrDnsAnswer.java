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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

/**
 * Reverse (PTR) DNS lookup response from {@link DnsClient}.
 */
@AutoValue
@WithBeanGetter
@JsonAutoDetect
@JsonDeserialize(builder = PtrDnsAnswer.Builder.class)
public abstract class PtrDnsAnswer implements DnsAnswer {

    public static final String FIELD_DOMAIN = "domain";
    public static final String FIELD_FULL_DOMAIN = "full_domain";
    public static final String FIELD_DNS_TTL = "dns_ttl";

    @JsonProperty(FIELD_DOMAIN)
    public abstract String domain();

    @JsonProperty(FIELD_FULL_DOMAIN)
    public abstract String fullDomain();

    @Override
    @JsonProperty(FIELD_DNS_TTL)
    public abstract long dnsTTL();

    public static Builder builder() {
        return new AutoValue_PtrDnsAnswer.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        @JsonCreator
        public static Builder createDefault() {
            return builder();
        }

        @JsonProperty(FIELD_DOMAIN)
        public abstract PtrDnsAnswer.Builder domain(String domain);

        @JsonProperty(FIELD_FULL_DOMAIN)
        public abstract PtrDnsAnswer.Builder fullDomain(String fullDomain);

        @JsonProperty(FIELD_DNS_TTL)
        public abstract PtrDnsAnswer.Builder dnsTTL(long dnsTTL);

        abstract PtrDnsAnswer autoBuild();

        public PtrDnsAnswer build() {

            return autoBuild();
        }
    }
}