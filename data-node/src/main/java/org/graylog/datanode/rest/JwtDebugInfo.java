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
package org.graylog.datanode.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

import java.util.Date;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class JwtDebugInfo {

    private final Date datanodeTime;
    private final Date issuedAt;
    private final Date expiration;
    @Nullable
    private final String error;

    public JwtDebugInfo(Date issuedAt, Date expiration, String error) {
        this.datanodeTime = new Date();
        this.issuedAt = issuedAt;
        this.expiration = expiration;
        this.error = error;
    }

    public JwtDebugInfo(Date issuedAt, Date expiration) {
        this(issuedAt, expiration, null);
    }

    public JwtDebugInfo(String error) {
        this(null, null, error);
    }

    @JsonProperty
    public Date datanodeTime() {
        return datanodeTime;
    }

    @JsonProperty
    public Date issuedAt() {
        return issuedAt;
    }

    @JsonProperty
    public Date expiration() {
        return expiration;
    }

    @JsonProperty
    @Nullable
    public String getError() {
        return error;
    }
}
