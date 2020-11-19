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
package org.graylog2.shared.security.tls;

import com.google.common.base.CharMatcher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a PEM file and converts it into a list of DERs so that they are imported into a {@link KeyStore} easily.
 */
final class PemReader {
    private static final Pattern CERT_PATTERN = Pattern.compile(
            "-+BEGIN\\s+.*CERTIFICATE[^-]*-+(?:\\s|\\r|\\n)+" + // Header
                    "([a-z0-9+/=\\r\\n]+)" +                    // Base64 text
                    "-+END\\s+.*CERTIFICATE[^-]*-+",            // Footer
            Pattern.CASE_INSENSITIVE);
    private static final Pattern KEY_PATTERN = Pattern.compile(
            "-+BEGIN\\s+.*PRIVATE\\s+KEY[^-]*-+(?:\\s|\\r|\\n)+" + // Header
                    "([a-z0-9+/=\\r\\n]+)" +                       // Base64 text
                    "-+END\\s+.*PRIVATE\\s+KEY[^-]*-+",            // Footer
            Pattern.CASE_INSENSITIVE);

    static List<byte[]> readCertificates(Path path) throws CertificateException {
        final byte[] bytes;
        try {
            bytes = Files.readAllBytes(path);
        } catch (IOException e) {
            throw new CertificateException("Couldn't read certificates from file: " + path, e);
        }

        final String content = new String(bytes, StandardCharsets.US_ASCII);
        final Matcher m = CERT_PATTERN.matcher(content);

        final List<byte[]> certs = new ArrayList<>();
        int start = 0;
        while (m.find(start)) {
            final String s = m.group(1);
            byte[] der = Base64.getDecoder().decode(CharMatcher.breakingWhitespace().removeFrom(s));
            certs.add(der);

            start = m.end();
        }

        if (certs.isEmpty()) {
            throw new CertificateException("No certificates found in file: " + path);
        }

        return certs;
    }

    static byte[] readPrivateKey(Path path) throws KeyException {
        final byte[] bytes;
        try {
            bytes = Files.readAllBytes(path);
        } catch (IOException e) {
            throw new KeyException("Couldn't read private key from file: " + path, e);
        }

        final String content = new String(bytes, StandardCharsets.US_ASCII);
        final Matcher m = KEY_PATTERN.matcher(content);
        if (!m.find()) {
            throw new KeyException("No private key found in file: " + path);
        }

        final String s = CharMatcher.breakingWhitespace().removeFrom(m.group(1));
        byte[] base64 = s.getBytes(StandardCharsets.US_ASCII);
        return Base64.getDecoder().decode(base64);
    }

    private PemReader() {
    }
}