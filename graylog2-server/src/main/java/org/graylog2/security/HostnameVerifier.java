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
package org.graylog2.security;

import org.bouncycastle.est.jcajce.JsseDefaultHostnameAuthorizer;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class HostnameVerifier {
    private final JsseDefaultHostnameAuthorizer authorizer;
    private final List<String> hosts;

    public HostnameVerifier(final String host) {
        this(List.of(host));
    }

    public HostnameVerifier(List<String> hosts) {
        this.hosts = hosts;
        this.authorizer = new JsseDefaultHostnameAuthorizer(Collections.emptySet());
    }

    public void validateHostnames(X509Certificate[] x509Certificates, String s) throws CertificateException {
        Arrays.stream(x509Certificates)
                .filter(this::certificateMatchesHostname)
                .findFirst()
                .orElseThrow(() -> new CertificateException("Presented certificate does not match configured hostname!"));
    }

    private boolean certificateMatchesHostname(X509Certificate x509Certificate) {
        return this.hosts.stream().anyMatch(host -> {
            try {
                return this.authorizer.verify(host, x509Certificate);
            } catch (IOException e) {
                return false;
            }
        });
    }
}
