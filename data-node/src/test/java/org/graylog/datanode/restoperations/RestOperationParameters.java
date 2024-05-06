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
import jakarta.inject.Provider;

import javax.annotation.Nullable;
import java.security.KeyStore;
import java.util.function.Supplier;

@AutoValue
public abstract class RestOperationParameters {

    abstract int port();

    @Nullable
    abstract KeyStore truststore();

    abstract Provider<String> jwtTokenProvider();

    private static final int DEFAULT_ATTEMPTS_COUNT = 160;

    abstract int attempts_count();

    public void addAuthorizationHeaders(RequestSpecification req) {
        if (jwtTokenProvider() != null) {
            req.header("Authorization", jwtTokenProvider().get());
        }
    }

    public String formatCurlAuthentication() {
        if (jwtTokenProvider() != null) {
            return "-H \"Authorization: " + jwtTokenProvider().get() + "\"";
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

        public abstract Builder jwtTokenProvider(Provider<String> jwtToken);

        public abstract Builder attempts_count(int attempts_count);

        public abstract RestOperationParameters build();
    }
}
