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
package org.graylog.security.certificates;

/**
 * Cryptographic algorithms supported for key pair generation.
 */
public enum Algorithm {
    ED25519("Ed25519", "Ed25519", 256),
    RSA_4096("RSA", "SHA256withRSA", 4096);

    private final String keyAlgorithm;
    private final String signatureAlgorithm;
    private final int keySize;

    Algorithm(String keyAlgorithm, String signatureAlgorithm, int keySize) {
        this.keyAlgorithm = keyAlgorithm;
        this.signatureAlgorithm = signatureAlgorithm;
        this.keySize = keySize;
    }

    public String keyAlgorithm() {
        return keyAlgorithm;
    }

    public String signatureAlgorithm() {
        return signatureAlgorithm;
    }

    public int keySize() {
        return keySize;
    }
}
