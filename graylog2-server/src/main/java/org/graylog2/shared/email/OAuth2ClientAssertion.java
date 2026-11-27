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
package org.graylog2.shared.email;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SecurityException;
import org.graylog2.plugin.inputs.transports.util.KeyUtil;

import javax.mail.MessagingException;

import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static java.util.Objects.requireNonNull;
import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Creates signed JWT client assertions (RFC 7523) that authenticate an OAuth 2.0 client with a certificate instead of
 * a client secret, in the format required by Microsoft Entra ID:
 * <a href="https://learn.microsoft.com/en-us/entra/identity-platform/certificate-credentials">certificate credentials</a>.
 * <p>
 * The certificate and private key are read for every assertion. Assertions are only needed when a new access token is
 * requested (about once an hour), and this way a renewed certificate is picked up without a restart.
 */
class OAuth2ClientAssertion {
    static final String ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

    /**
     * The assertion is sent right away, so it doesn't have to be valid for long. Microsoft recommends at most 5-10 minutes.
     */
    static final Duration LIFETIME = Duration.ofMinutes(5);

    private final Clock clock;

    OAuth2ClientAssertion(Clock clock) {
        this.clock = requireNonNull(clock, "clock");
    }

    /**
     * @param clientId    the OAuth 2.0 client ID, used as issuer and subject
     * @param audience    the token endpoint the assertion is sent to
     * @param certificate the PEM certificate registered with the client
     * @param privateKey  the PEM private key belonging to the certificate
     * @param password    the password of the private key, or {@code null} if it isn't encrypted
     * @throws MessagingException if the certificate or key can't be used
     */
    String create(String clientId, String audience, Path certificate, Path privateKey, String password) throws MessagingException {
        final Instant now = clock.instant();
        final X509Certificate cert = readCertificate(certificate, now);
        final RSAPrivateKey key = readPrivateKey(privateKey, password, cert);

        try {
            return Jwts.builder()
                    .header()
                    .type("JWT")
                    .x509Sha256Thumbprint(MessageDigest.getInstance("SHA-256").digest(cert.getEncoded()))
                    .and()
                    .issuer(clientId)
                    .subject(clientId)
                    .audience().single(audience)
                    .id(UUID.randomUUID().toString())
                    .issuedAt(Date.from(now))
                    .notBefore(Date.from(now))
                    .expiration(Date.from(now.plus(LIFETIME)))
                    .signWith(key, Jwts.SIG.PS256)
                    .compact();
        } catch (GeneralSecurityException | SecurityException e) {
            throw new MessagingException(f("Failed to sign OAuth 2.0 client assertion with private key <%s>: %s",
                    privateKey, e.getMessage()), e);
        }
    }

    private static X509Certificate readCertificate(Path path, Instant now) throws MessagingException {
        final X509Certificate[] certificates;
        try {
            certificates = KeyUtil.loadX509Certificates(path);
        } catch (Exception e) {
            throw new MessagingException(f("Failed to read OAuth 2.0 client certificate <%s>: %s", path, e.getMessage()), e);
        }
        if (certificates.length == 0) {
            throw new MessagingException(f("OAuth 2.0 client certificate file <%s> does not contain a certificate", path));
        }
        // The first certificate is the client's own one, any further ones are its issuers.
        final X509Certificate certificate = certificates[0];
        try {
            certificate.checkValidity(Date.from(now));
        } catch (CertificateExpiredException e) {
            throw new MessagingException(f("OAuth 2.0 client certificate <%s> expired on %s", path,
                    certificate.getNotAfter().toInstant()));
        } catch (CertificateNotYetValidException e) {
            throw new MessagingException(f("OAuth 2.0 client certificate <%s> is not valid before %s", path,
                    certificate.getNotBefore().toInstant()));
        }
        return certificate;
    }

    private static RSAPrivateKey readPrivateKey(Path path, String password, X509Certificate certificate) throws MessagingException {
        final PrivateKey key;
        try {
            key = KeyUtil.privateKeyFromFile(password, path.toFile());
        } catch (Exception e) {
            // Also covers files that don't contain a PEM object at all.
            throw new MessagingException(f("Failed to read OAuth 2.0 client private key <%s>: %s", path, e.getMessage()), e);
        }
        if (!(key instanceof RSAPrivateKey rsaKey) || !(certificate.getPublicKey() instanceof RSAPublicKey publicKey)) {
            throw new MessagingException(f("OAuth 2.0 client certificate and private key <%s> must use RSA", path));
        }
        if (rsaKey instanceof RSAPrivateCrtKey crtKey && !crtKey.getModulus().equals(publicKey.getModulus())) {
            throw new MessagingException(f("OAuth 2.0 client private key <%s> does not belong to the certificate", path));
        }
        return rsaKey;
    }
}
