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
package org.graylog.datanode.configuration.variants;

import jakarta.inject.Inject;
import org.apache.commons.lang3.RandomStringUtils;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.datanode.configuration.OpensearchConfigurationException;
import org.graylog.security.certutil.keystore.storage.KeystoreUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.graylog.datanode.Configuration.HTTP_CERTIFICATE_PASSWORD_PROPERTY;
import static org.graylog.datanode.Configuration.TRANSPORT_CERTIFICATE_PASSWORD_PROPERTY;
import static org.graylog.security.certutil.CertConstants.PKCS12;

public final class LocalConfigurationCertificatesProvider implements OpensearchCertificatesProvider {

    private final String tranportCertificateFile;
    private final String httpCertificateFile;
    private final String transportCertificatePassword;
    private final String httpCertificatePassword;
    private final DatanodeConfiguration datanodeConfiguration;
    private final String transportCertificateAlias;
    private final String httpCertificateAlias;

    @Inject
    public LocalConfigurationCertificatesProvider(final Configuration localConfiguration,
                                                  final DatanodeConfiguration datanodeConfiguration) {
        this.datanodeConfiguration = datanodeConfiguration;

        this.tranportCertificateFile = localConfiguration.getDatanodeTransportCertificate();
        this.transportCertificatePassword = localConfiguration.getDatanodeTransportCertificatePassword();
        this.transportCertificateAlias = localConfiguration.getDatanodeTransportCertificateAlias();

        this.httpCertificateFile = localConfiguration.getDatanodeHttpCertificate();
        this.httpCertificatePassword = localConfiguration.getDatanodeHttpCertificatePassword();
        this.httpCertificateAlias = localConfiguration.getDatanodeHttpCertificateAlias();
    }

    @Override
    public boolean isConfigured(Configuration localConfiguration) throws OpensearchConfigurationException {

        if (noneOfRequiredConfigOptionsProvided()) {
            return false; // none of the uploaded cert options is provided => not usable for this security config, skip this config
        }

        List<String> errors = new LinkedList<>();

        if (isBlank(transportCertificatePassword)) {
            errors.add(TRANSPORT_CERTIFICATE_PASSWORD_PROPERTY + " required. Please configure password to your transport certificates keystore.");
        }

        if (!fileExists(tranportCertificateFile)) {
            errors.add("transport_certificate required. Please provide a path to a certificate file in your configuration.");
        }

        if (isBlank(httpCertificatePassword)) {
            errors.add(HTTP_CERTIFICATE_PASSWORD_PROPERTY + " required. Please configure password to your http certificates keystore.");
        }

        if (!fileExists(httpCertificateFile)) {
            errors.add("http_certificate required. Please provide a path to a certificate file in your configuration.");
        }

        if (!errors.isEmpty()) {
            throw new OpensearchConfigurationException("Configuration incomplete, check the following settings: " + String.join(", ", errors));
        }

        return true;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean fileExists(String filename) {
        return Optional.ofNullable(filename)
                .flatMap(fileName -> datanodeConfiguration.datanodeDirectories().resolveConfigurationSourceFile(filename))
                .map(Files::exists)
                .orElse(false);
    }

    /**
     * We require either full set of http and transport certificates and their keys or nothing. Anything in-between will
     * lead to an exception, it's a mismatched configuration and would cause problems in the future.
     */
    private boolean noneOfRequiredConfigOptionsProvided() {
        return isBlank(transportCertificatePassword) &&
                isBlank(httpCertificatePassword) &&
                isBlank(httpCertificateFile) &&
                isBlank(tranportCertificateFile);
    }

    @Override
    public OpensearchCertificates build() {
        final Path transportCertPath = datanodeConfiguration.datanodeDirectories().resolveConfigurationSourceFile(tranportCertificateFile).orElseThrow(() -> new RuntimeException("This should not happen, certificate expected"));
        final Path httpCertPath = datanodeConfiguration.datanodeDirectories().resolveConfigurationSourceFile(httpCertificateFile).orElseThrow(() -> new RuntimeException("This should not happen, certificate expected"));
        final char[] oneTimePassword = RandomStringUtils.secure().nextAlphabetic(256).toCharArray();
        return new OpensearchCertificates(oneTimePassword, createSupplier(httpCertPath, httpCertificatePassword, oneTimePassword), httpCertificateAlias, createSupplier(transportCertPath, transportCertificatePassword, oneTimePassword), transportCertificateAlias);
    }

    public Supplier<KeyStore> createSupplier(Path keystorePath, String originalPassword, char[] newPassword) {
        return () -> {
            try {
                final KeyStore keystore = loadKeystore(keystorePath, originalPassword);
                return KeystoreUtils.newStoreCopyContent(keystore, originalPassword.toCharArray(), newPassword);
            } catch (IOException | GeneralSecurityException e) {
                throw new RuntimeException("Failed to load keystore from path " + keystorePath, e);
            }
        };
    }

    public KeyStore loadKeystore(Path file, String password) throws IOException, GeneralSecurityException {
        KeyStore keyStore = KeyStore.getInstance(PKCS12);
        try (FileInputStream fis = new FileInputStream(file.toFile())) {
            keyStore.load(fis, password.toCharArray());
            return keyStore;
        }
    }
}
