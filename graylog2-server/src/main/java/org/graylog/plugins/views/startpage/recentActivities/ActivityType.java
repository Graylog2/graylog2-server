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
package org.graylog.plugins.views.startpage.recentActivities;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ActivityType {
    @JsonProperty("create")
    CREATE("create"),
    @JsonProperty("delete")
    DELETE("delete"),
    @JsonProperty("update")
    UPDATE("update"),
    @JsonProperty("share")
    SHARE("share"),
    @JsonProperty("unshare")
    UNSHARE("unshare");

    private final String activity;

    ActivityType(final String type) {
        this.activity = type;
    }

    @Override
    public String toString() {
        return switch (this) {
            case CREATE -> "create";
            case DELETE -> "delete";
            case UPDATE -> "update";
            case SHARE -> "share";
            case UNSHARE -> "unshare";
        };
    }

}
