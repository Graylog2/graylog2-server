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
package org.graylog.freeenterprise;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.joda.time.DateTime;

@AutoValue
@JsonDeserialize(builder = StagedFreeEnterpriseLicense.Builder.class)
public abstract class StagedFreeEnterpriseLicense {
    private static final String FIELD_CLUSTER_ID = "cluster_id";
    private static final String FIELD_LICENSE = "license";
    private static final String FIELD_CREATED_AT = "created_at";

    @JsonProperty(FIELD_CLUSTER_ID)
    public abstract String clusterId();

    @JsonProperty(FIELD_LICENSE)
    public abstract String license();

    @JsonProperty(FIELD_CREATED_AT)
    public abstract DateTime createdAt();

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_StagedFreeEnterpriseLicense.Builder();
        }

        @JsonProperty(FIELD_CLUSTER_ID)
        public abstract Builder clusterId(String clusterId);

        @JsonProperty(FIELD_LICENSE)
        public abstract Builder license(String license);

        @JsonProperty(FIELD_CREATED_AT)
        public abstract Builder createdAt(DateTime createdAt);

        public abstract StagedFreeEnterpriseLicense build();
    }
}
