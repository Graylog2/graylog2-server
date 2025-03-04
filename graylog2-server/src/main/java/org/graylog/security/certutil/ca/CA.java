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
package org.graylog.security.certutil.ca;

import org.graylog.security.certutil.keystore.storage.KeystoreUtils;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CA {

    private List<X509Certificate> certificates;
    private final PrivateKey privateKey;

    public CA(List<X509Certificate> certificates, PrivateKey privateKey) {
        this.certificates = certificates;
        this.privateKey = privateKey;
        validateCertificates();
    }

    private void validateCertificates() {
        if (certificates == null || certificates.isEmpty()) {
            throw new IllegalArgumentException("Certificate list is empty");
        }
        if (certificates.size() > 1 && !isCertPathOrderedCorrectly()) {
            this.certificates = sortCertificates(certificates);
        }
        final X509Certificate ca = certificates.get(0);
        if (ca.getBasicConstraints() == -1) {
            throw new IllegalArgumentException("First certificate in certificate chain is no CA. Please make sure that your bundle only contains the CA and necessary intermediate/root certificates");
        }

        try {
            if (!KeystoreUtils.matchingKeys(privateKey, ca.getPublicKey())) {
                throw new IllegalArgumentException("Provided CA private key doesn't correspond to provided CA certificate");
            }
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isCertPathOrderedCorrectly() {
        for (int i = 0; i < certificates.size() - 1; i++) {
            X509Certificate currentCert = certificates.get(i);
            X509Certificate nextCert = certificates.get(i + 1);
            if (!currentCert.getIssuerX500Principal().equals(nextCert.getSubjectX500Principal())) {
                return false;
            }
        }
        return true;
    }

    private List<X509Certificate> sortCertificates(List<X509Certificate> certificates) {
        X509Certificate[] sorted = new X509Certificate[certificates.size()];
        certificates.forEach(cert -> {
            int position = getChainPosition(cert);
            if (sorted[position] != null) { // multiple certificates at same position in chain
                throw new IllegalArgumentException("Corrupt certificate chain. Please make sure that your bundle only contains the CA and necessary intermediate/root certificates");
            }
            sorted[position] = cert;
        });
        List<X509Certificate> sortedList = Arrays.asList(sorted);
        Collections.reverse(sortedList);
        return sortedList;
    }

    private int getChainPosition(X509Certificate cert) {
        int position = 0;
        X509Certificate currentCert = cert;
        for (int i = 0; i < certificates.size(); i++) { // loop protection
            X509Certificate issuerCert = findIssuer(currentCert);
            if (issuerCert == null) {
                return position;
            }
            currentCert = issuerCert;
            position++;
        }
        throw new IllegalArgumentException("Corrupt certificate chain containing a signing loop.");
    }

    private X509Certificate findIssuer(X509Certificate cert) {
        return certificates.stream().filter(potentialIssuer ->
                        !potentialIssuer.getSubjectX500Principal().equals(cert.getSubjectX500Principal()) &&
                                potentialIssuer.getSubjectX500Principal().equals(cert.getIssuerX500Principal()))
                .findFirst()
                .orElse(null);
    }

    public List<X509Certificate> getCertificates() {
        return certificates;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }
}
