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
package org.graylog.security.certutil;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class CertRequest {

    /**
     * The CN={name} of the certificate. When the certificate is for a domain it should be the domain name
     */
    private final String cnName;
    private final List<String> subjectAlternativeNames = new ArrayList<>();
    /**
     * Issuer who signs this certificate. Null for a self-signed certificate
     */
    private final KeyPair issuer;
    /**
     * Can this certificate be used to sign other certificates?
     */
    private boolean isCA = false;
    private Duration validity;

    public static CertRequest selfSigned(String cnName) {
        return new CertRequest(cnName, null);
    }

    public static CertRequest signed(String cnName, KeyPair issuer) {
        return new CertRequest(cnName, issuer);
    }

    private CertRequest(String cnName, KeyPair issuer) {
        this.cnName = cnName;
        this.issuer = issuer;
    }

    public CertRequest isCA(boolean isCA) {
        this.isCA = isCA;
        return this;
    }

    public CertRequest withSubjectAlternativeName(String name) {
        this.subjectAlternativeNames.add(name);
        return this;
    }

    public String cnName() {
        return cnName;
    }

    public List<String> subjectAlternativeNames() {
        return subjectAlternativeNames;
    }

    public KeyPair issuer() {
        return issuer;
    }

    public boolean isCA() {
        return isCA;
    }

    public CertRequest validity(Duration certificateValidity) {
        this.validity = certificateValidity;
        return this;
    }

    public Duration validity() {
        return validity;
    }
}
