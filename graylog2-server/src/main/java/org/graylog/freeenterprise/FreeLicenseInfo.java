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
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class FreeLicenseInfo {
    public enum Status {
        @JsonProperty("absent")
        ABSENT,
        @JsonProperty("staged")
        STAGED,
        @JsonProperty("installed")
        INSTALLED
    }

    private static final String FIELD_LICENSE_STATUS = "license_status";

    @JsonProperty(FIELD_LICENSE_STATUS)
    public abstract Status licenseStatus();

    public static FreeLicenseInfo absent() {
        return create(Status.ABSENT);
    }

    public static FreeLicenseInfo staged() {
        return create(Status.STAGED);
    }

    public static FreeLicenseInfo installed() {
        return create(Status.INSTALLED);
    }

    @JsonCreator
    public static FreeLicenseInfo create(@JsonProperty(FIELD_LICENSE_STATUS) Status licenseStatus) {
        return new AutoValue_FreeLicenseInfo(licenseStatus);
    }
}
