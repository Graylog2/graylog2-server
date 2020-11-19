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
package org.graylog.security.permissions;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.auto.value.AutoValue;
import org.apache.shiro.authz.Permission;
import org.graylog.grn.GRN;

@AutoValue
public abstract class GRNPermission implements Permission {
    public abstract String type();

    public abstract GRN target();

    public static GRNPermission create(String type, GRN target) {
        return new AutoValue_GRNPermission(type, target);
    }

    @Override
    public boolean implies(Permission p) {
        // GRNPermissions only supports comparisons with other GRNPermissions
        if (!(p instanceof GRNPermission)) {
            return false;
        }
        GRNPermission other = (GRNPermission) p;

        return (other.type().equals(type()) && other.target().equals(target()));
    }

    @JsonValue
    // This string representation is used in the UserSummary and exported to the frontend
    public String jsonValue() {
        return type() + ":" + target().toString();
    }
}
