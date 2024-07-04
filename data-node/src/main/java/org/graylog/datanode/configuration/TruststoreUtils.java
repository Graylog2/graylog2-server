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
package org.graylog.datanode.configuration;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Optional;
import java.util.stream.Stream;

public class TruststoreUtils {

    private static final Logger LOG = LoggerFactory.getLogger(TruststoreUtils.class);

    public static Optional<KeyStore> loadJvmTruststore() {
        return jvmTruststoreLocation().map(location -> {

            String password = jvmTruststorePassword();
            String type = jvmTruststoreType();

            LOG.info("Detected existing JVM truststore: " + location.toAbsolutePath() + " of type " + type);

            try {
                KeyStore trustStore = KeyStore.getInstance(type);
                try (InputStream is = Files.newInputStream(location)) {
                    trustStore.load(is, password.toCharArray());
                }
                return trustStore;
            } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static String jvmTruststoreType() {
        return Optional.ofNullable(System.getProperty("javax.net.ssl.trustStoreType"))
                .filter(type -> !type.isEmpty())
                .orElseGet(KeyStore::getDefaultType);
    }

    private static String jvmTruststorePassword() {
        return Optional.ofNullable(System.getProperty("javax.net.ssl.trustStorePassword"))
                .filter(p -> !p.isEmpty())
                .orElse("changeit");
    }

    private static Optional<Path> jvmTruststoreLocation() {
        String javaHome = System.getProperty("java.home");
        return Stream.of(
                        truststoreSystemProperty(),
                        libSecurityFile(javaHome, "jssecacerts"),
                        libSecurityFile(javaHome, "cacerts")
                )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(TruststoreUtils::isReadableFile)
                .findFirst();
    }

    @Nonnull
    private static Optional<Path> libSecurityFile(String javaHome, String filename) {
        return Optional.ofNullable(javaHome).map(home -> Paths.get(home, "lib", "security", filename));
    }

    @Nonnull
    private static Optional<Path> truststoreSystemProperty() {
        return Optional.ofNullable(System.getProperty("javax.net.ssl.trustStore")).filter(p -> !p.isEmpty()).map(Paths::get);
    }

    private static boolean isReadableFile(Path path) {
        return Files.exists(path) && Files.isRegularFile(path) && Files.isReadable(path);
    }
}
