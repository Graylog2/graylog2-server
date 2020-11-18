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
package org.graylog.freeenterprise;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import javax.validation.constraints.Email;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

@AutoValue
@JsonDeserialize(builder = FreeLicenseAPIRequest.Builder.class)
public abstract class FreeLicenseAPIRequest {
    public static final String FIELD_CLUSTER_ID = "cluster_id";
    public static final String FIELD_FIRST_NAME = "first_name";
    public static final String FIELD_LAST_NAME = "last_name";
    public static final String FIELD_EMAIL = "email";
    public static final String FIELD_PHONE = "phone";
    public static final String FIELD_COMPANY = "company";
    public static final String FIELD_VERSION = "version";

    public static final int DEFAULT_VERSION = 1;

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

    @JsonProperty(FIELD_PHONE)
    @NotBlank
    public abstract String phone();

    @JsonProperty(FIELD_COMPANY)
    @NotBlank
    public abstract String company();

    @JsonProperty(FIELD_VERSION)
    @Min(1)
    public abstract int version();

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_FreeLicenseAPIRequest.Builder().version(DEFAULT_VERSION);
        }

        @JsonProperty(FIELD_CLUSTER_ID)
        public abstract Builder clusterId(@NotBlank String clusterId);

        @JsonProperty(FIELD_FIRST_NAME)
        public abstract Builder firstName(@NotBlank String firstName);

        @JsonProperty(FIELD_LAST_NAME)
        public abstract Builder lastName(@NotBlank String lastName);

        @JsonProperty(FIELD_EMAIL)
        public abstract Builder email(@NotBlank @Email String email);

        @JsonProperty(FIELD_PHONE)
        public abstract Builder phone(@NotBlank String phone);

        @JsonProperty(FIELD_COMPANY)
        public abstract Builder company(@NotBlank String company);

        @JsonProperty(FIELD_VERSION)
        public abstract Builder version(int version);

        public abstract FreeLicenseAPIRequest build();
    }
}
