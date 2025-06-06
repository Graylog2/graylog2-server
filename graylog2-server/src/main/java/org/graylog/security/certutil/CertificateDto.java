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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public record CertificateDto(String subject, String issuer,
                             BigInteger serialNumber,
                             Date validFrom, Date validTo, List<String> subjectAlternativeNames) {

    private static final Logger log = LoggerFactory.getLogger(CertificateDto.class);

    public CertificateDto(X509Certificate certificate) {
        this(certificate.getSubjectX500Principal().getName(), certificate.getIssuerX500Principal().getName(), certificate.getSerialNumber(),
                certificate.getNotBefore(), certificate.getNotAfter(), getSubjectAlternativeNames(certificate));
    }

    private static List<String> getSubjectAlternativeNames(X509Certificate certificate) {
        ArrayList<String> names = new ArrayList<>();
        try {
            Collection<List<?>> altNames = certificate.getSubjectAlternativeNames();
            if (altNames == null) {
                return names;
            }

            for (List<?> altName : altNames) {
                if (altName.size() >= 2) {
                    Object nameValue = altName.get(1);
                    if (nameValue instanceof String) {
                        names.add((String) nameValue);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error getting subject alternative names", e);
        }
        return names;
    }

}
