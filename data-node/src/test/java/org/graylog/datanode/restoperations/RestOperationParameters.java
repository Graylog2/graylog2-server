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
package org.graylog.datanode.restoperations;

import com.google.auto.value.AutoValue;
import io.restassured.specification.RequestSpecification;

import javax.annotation.Nullable;
import java.security.KeyStore;

@AutoValue
public abstract class RestOperationParameters {

    abstract int port();

    @Nullable
    abstract KeyStore truststore();

    @Nullable
    abstract String username();

    @Nullable
    abstract String password();

    @Nullable
    abstract String jwtToken();


    private static final int DEFAULT_ATTEMPTS_COUNT = 160;

    abstract int attempts_count();

    public void addAuthorizationHeaders(RequestSpecification req) {
        if (jwtToken() != null) {
            req.header("Authorization", "Bearer " + jwtToken());
        } else if (username() != null && password() != null) {
            req.auth().basic(username(), password());
        }
    }

    public String formatCurlAuthentication() {
        if (jwtToken() != null) {
            return "-H \"Authorization: Bearer " + jwtToken() + "\"";
        } else if (username() != null && password() != null) {
            return "-u \"" + username() + ":" + password() + "\"";
        }
        return "";
    }

    public static Builder builder() {
        return new AutoValue_RestOperationParameters.Builder()
                .attempts_count(DEFAULT_ATTEMPTS_COUNT);
    }

    abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder port(int port);

        public abstract Builder truststore(KeyStore truststore);

        public abstract Builder username(String username);

        public abstract Builder password(String password);

        public abstract Builder jwtToken(String jwtToken);


        public abstract Builder attempts_count(int attempts_count);

        public abstract RestOperationParameters build();
    }
}
