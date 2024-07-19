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
package org.graylog2.security.untrusted;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Wrapper around CertificateException which additionally makes the actual certificate chain accessible to the caller/
 * catcher of the exception.
 */
public class CertificateReportingException extends CertificateException {
    private final X509Certificate[] chain;

    public CertificateReportingException(CertificateException cause, X509Certificate[] chain) {
        super(cause);
        this.chain = chain;
    }

    public X509Certificate[] getChain() {
        return chain;
    }
}
