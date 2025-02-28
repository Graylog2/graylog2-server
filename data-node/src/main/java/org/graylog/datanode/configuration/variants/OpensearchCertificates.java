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
import org.graylog.security.certutil.csr.KeystoreInformation;

public class OpensearchCertificates {

    @Nullable
    private final KeystoreInformation transportCertificate;
    @Nullable
    private final String transportKeyAlias;
    @Nullable
    private final KeystoreInformation httpCertificate;
    @Nullable
    private final String httpKeyAlias;

    public OpensearchCertificates(@Nullable KeystoreInformation transportCertificate, @Nullable String transportKeyAlias, @Nullable KeystoreInformation httpCertificate, @Nullable String httpKeyAlias) {
        this.transportCertificate = transportCertificate;
        this.transportKeyAlias = transportKeyAlias;
        this.httpCertificate = httpCertificate;
        this.httpKeyAlias = httpKeyAlias;
    }

    public OpensearchCertificates(KeystoreInformation transportCertificate, KeystoreInformation httpCertificate) {
        // null aliases mean autodetection - first alias will be used
        this(transportCertificate, null, httpCertificate, null);
    }

    public static OpensearchCertificates none() {
        return new OpensearchCertificates(null, null, null, null);
    }

    @Nullable
    public KeystoreInformation getTransportCertificate() {
        return transportCertificate;
    }

    @Nullable
    public KeystoreInformation getHttpCertificate() {
        return httpCertificate;
    }

    @Nullable
    public String getTransportKeyAlias() {
        return transportKeyAlias;
    }

    @Nullable
    public String getHttpKeyAlias() {
        return httpKeyAlias;
    }

    public boolean hasBothCertificates() {
        return getHttpCertificate() != null && getTransportCertificate() != null;
    }
}
