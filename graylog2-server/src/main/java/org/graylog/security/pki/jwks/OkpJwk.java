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
package org.graylog.security.pki.jwks;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.security.interfaces.EdECPublicKey;
import java.util.Base64;

/**
 * JWK representation for Octet Key Pair (OKP) keys.
 * <p>
 * Supports Ed25519 and Ed448 curves as defined in RFC 8037.
 *
 * @param kid the key ID (certificate fingerprint)
 * @param crv the curve name ("Ed25519" or "Ed448")
 * @param x the base64url-encoded public key
 * @param use the key use ("sig" for signing)
 */
public record OkpJwk(
        @JsonProperty("kid") String kid,
        @JsonProperty("crv") String crv,
        @JsonProperty("x") String x,
        @JsonProperty("use") String use
) implements Jwk {

    /**
     * Ed25519 SubjectPublicKeyInfo ASN.1 header length.
     * The header is: SEQUENCE { SEQUENCE { OID 1.3.101.112 } BIT STRING { ... } }
     * Total header length is 12 bytes before the 32-byte raw public key.
     */
    private static final int ED25519_SPKI_HEADER_LENGTH = 12;

    /**
     * Expected length of raw Ed25519 public key.
     */
    private static final int ED25519_PUBLIC_KEY_LENGTH = 32;

    @Override
    @JsonProperty("kty")
    public String kty() {
        return "OKP";
    }

    /**
     * Creates an OkpJwk from an EdEC public key.
     *
     * @param kid the key ID (certificate fingerprint)
     * @param publicKey the EdEC public key
     * @return the JWK representation
     * @throws IllegalArgumentException if the key format is invalid
     */
    public static OkpJwk fromPublicKey(String kid, EdECPublicKey publicKey) {
        final String crv = detectCurve(publicKey);
        final String x = extractPublicKeyBase64Url(publicKey);
        return new OkpJwk(kid, crv, x, "sig");
    }

    private static String detectCurve(EdECPublicKey publicKey) {
        final String algorithm = publicKey.getAlgorithm();
        return switch (algorithm) {
            case "Ed25519", "EdDSA" -> "Ed25519";
            case "Ed448" -> "Ed448";
            default -> throw new IllegalArgumentException("Unsupported EdEC algorithm: " + algorithm);
        };
    }

    /**
     * Extracts the raw public key from an EdEC public key and encodes it as base64url.
     * <p>
     * EdEC public keys in Java are encoded in SubjectPublicKeyInfo (SPKI) format,
     * which includes an ASN.1 header. This method strips the header to get
     * the raw public key as required by JWK format.
     */
    private static String extractPublicKeyBase64Url(EdECPublicKey publicKey) {
        final byte[] encoded = publicKey.getEncoded();
        final int expectedLength = ED25519_SPKI_HEADER_LENGTH + ED25519_PUBLIC_KEY_LENGTH;

        if (encoded.length != expectedLength) {
            throw new IllegalArgumentException(
                    "Invalid Ed25519 public key length: expected " + expectedLength + ", got " + encoded.length
            );
        }

        // Extract raw public key by skipping the ASN.1 header
        final byte[] rawPublicKey = new byte[ED25519_PUBLIC_KEY_LENGTH];
        System.arraycopy(encoded, ED25519_SPKI_HEADER_LENGTH, rawPublicKey, 0, ED25519_PUBLIC_KEY_LENGTH);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(rawPublicKey);
    }
}
