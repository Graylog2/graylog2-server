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
package org.graylog.security.shares;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;

@AutoValue
public abstract class Grantee {

    public static final String GRANTEE_TYPE_GLOBAL = "global";
    public static final String GRANTEE_TYPE_USER = "user";

    @JsonProperty("id")
    public abstract GRN grn();

    @JsonProperty("type")
    public abstract String type();

    @JsonProperty("title")
    public abstract String title();

    public static Grantee create(GRN grn, String type, String title) {
        return new AutoValue_Grantee(grn, type, title);
    }

    public static Grantee createGlobal() {
        return create(GRNRegistry.GLOBAL_USER_GRN, GRANTEE_TYPE_GLOBAL, "Everyone");
    }

    public static Grantee createUser(GRN grn, String title) {
        return create(grn, GRANTEE_TYPE_USER, title);
    }

}
