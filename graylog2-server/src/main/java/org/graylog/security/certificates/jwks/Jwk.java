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
package org.graylog.security.certificates.jwks;

/**
 * Interface for JSON Web Key (JWK) representations.
 * <p>
 * Implementations provide specific key types as defined in the
 * <a href="https://www.iana.org/assignments/jose/jose.xhtml#web-key-types">IANA JOSE registry</a>:
 * <ul>
 *   <li>{@link OkpJwk} - Octet Key Pair (Ed25519, Ed448)</li>
 * </ul>
 * <p>
 * Additional key types (RSA, EC) can be added by implementing this interface.
 */
public interface Jwk {

    /**
     * Returns the key type (kty) parameter.
     * <p>
     * Standard values: "RSA", "EC", "OKP", "oct"
     *
     * @return the key type
     */
    String kty();

    /**
     * Returns the key ID (kid) parameter.
     * <p>
     * We use the certificate fingerprint (sha256:...) as the key ID.
     *
     * @return the key ID
     */
    String kid();

    /**
     * Returns the public key use (use) parameter.
     * <p>
     * For signing keys, this is "sig".
     *
     * @return the key use
     */
    String use();
}
