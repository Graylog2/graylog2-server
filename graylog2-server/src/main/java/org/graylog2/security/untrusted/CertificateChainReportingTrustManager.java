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

import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * This trust manager is making the certificate chain easily accessible in the CertificateReportingException if the
 * delegate trust manager throws an certificate exception. The caller can then see which certificates have been
 * used and validated.
 */
public class CertificateChainReportingTrustManager implements X509TrustManager {

    private final X509TrustManager delegate;

    public CertificateChainReportingTrustManager(X509TrustManager delegate) {
        this.delegate = delegate;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        try {
            delegate.checkClientTrusted(chain, authType);
        } catch (CertificateException e) {
            throw new CertificateReportingException(e, chain);
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        try {
            delegate.checkServerTrusted(chain, authType);
        } catch (CertificateException e) {
            throw new CertificateReportingException(e, chain);
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return delegate.getAcceptedIssuers();
    }
}
