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

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@AutoValue
@JsonDeserialize(builder = FreeLicenseRequest.Builder.class)
public abstract class FreeLicenseRequest {
    public static final String FIELD_CLUSTER_ID = "cluster_id";
    public static final String FIELD_FIRST_NAME = "first_name";
    public static final String FIELD_LAST_NAME = "last_name";
    public static final String FIELD_EMAIL = "email";
    public static final String FIELD_COMPANY = "company";

    @JsonProperty(FIELD_CLUSTER_ID)
    @NotBlank
    public abstract String clusterId();

    @JsonProperty(FIELD_FIRST_NAME)
    @NotBlank
    public abstract String firstName();

    @JsonProperty(FIELD_LAST_NAME)
    @NotBlank
    public abstract String lastName();

    @JsonProperty(FIELD_EMAIL)
    @NotBlank
    @Email
    public abstract String email();

    @JsonProperty(FIELD_COMPANY)
    @NotBlank
    public abstract String company();

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_FreeLicenseRequest.Builder();
        }

        @JsonProperty(FIELD_CLUSTER_ID)
        public abstract Builder clusterId(@NotBlank String clusterId);

        @JsonProperty(FIELD_FIRST_NAME)
        public abstract Builder firstName(@NotBlank String firstName);

        @JsonProperty(FIELD_LAST_NAME)
        public abstract Builder lastName(@NotBlank String lastName);

        @JsonProperty(FIELD_EMAIL)
        public abstract Builder email(@NotBlank @Email String email);

        @JsonProperty(FIELD_COMPANY)
        public abstract Builder company(@NotBlank String company);

        public abstract FreeLicenseRequest build();
    }
}
