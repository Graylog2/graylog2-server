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
package org.graylog.datanode.configuration.variants;

import jakarta.annotation.Nullable;

import java.security.KeyStore;
import java.util.function.Supplier;

public class OpensearchCertificates {

    private final Supplier<KeyStore> httpKeystore;
    private final Supplier<KeyStore> transportKeystore;

    @Nullable
    private final String transportKeyAlias;
    @Nullable
    private final String httpKeyAlias;


    private final char[] password;

    public OpensearchCertificates(char[] password, Supplier<KeyStore> httpKeystore, @Nullable String httpKeyAlias, Supplier<KeyStore> transportKeystore, @Nullable String transportKeyAlias) {
        this.httpKeystore = httpKeystore;
        this.transportKeystore = transportKeystore;
        this.transportKeyAlias = transportKeyAlias;
        this.httpKeyAlias = httpKeyAlias;
        this.password = password;
    }

    public static OpensearchCertificates none() {
        return new OpensearchCertificates(null, null, null, null, null);
    }

    public Supplier<KeyStore> getHttpKeystore() {
        return httpKeystore;
    }

    public Supplier<KeyStore> getTransportKeystore() {
        return transportKeystore;
    }

    @Nullable
    public String getTransportKeyAlias() {
        return transportKeyAlias;
    }

    @Nullable
    public String getHttpKeyAlias() {
        return httpKeyAlias;
    }

    public char[] getPassword() {
        return password;
    }

    public boolean hasCertificates() {
        return getHttpKeystore() != null && getTransportKeystore() != null;
    }
}
