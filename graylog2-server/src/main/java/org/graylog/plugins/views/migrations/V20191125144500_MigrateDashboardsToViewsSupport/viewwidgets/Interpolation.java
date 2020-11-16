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
package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Interpolation {
    linear("linear"),
    stepAfter("step-after"),
    spline("spline");

    private final String value;

    @JsonValue
    public String value() {
        return this.value;
    }

    public static Interpolation defaultValue() {
        return linear;
    }

    public static Interpolation fromLegacyValue(String legacyValue) {
        switch (legacyValue) {
            case "linear": return linear;
            case "step-after": return stepAfter;
            case "cardinal":
            case "basis":
            case "bundle":
            case "monotone": return spline;
        }
        throw new RuntimeException("Invalid interpolation value: " + legacyValue);
    }

    Interpolation(String value) {
        this.value = value;
    }
}
