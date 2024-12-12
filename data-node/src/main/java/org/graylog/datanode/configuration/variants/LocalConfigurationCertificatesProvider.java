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

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.apache.commons.lang3.RandomStringUtils;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.datanode.configuration.OpensearchConfigurationException;
import org.graylog.security.certutil.csr.FilesystemKeystoreInformation;
import org.graylog.security.certutil.csr.InMemoryKeystoreInformation;
import org.graylog.security.certutil.csr.KeystoreInformation;
import org.graylog.security.certutil.keystore.storage.KeystoreUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static org.graylog.datanode.Configuration.HTTP_CERTIFICATE_PASSWORD_PROPERTY;
import static org.graylog.datanode.Configuration.TRANSPORT_CERTIFICATE_PASSWORD_PROPERTY;

public final class LocalConfigurationCertificatesProvider implements OpensearchCertificatesProvider {

    private final String tranportCertificateFile;
    private final String httpCertificateFile;
    private final String transportCertificatePassword;
    private final String httpCertificatePassword;
    private final DatanodeConfiguration datanodeConfiguration;

    @Inject
    public LocalConfigurationCertificatesProvider(final Configuration localConfiguration,
                                                  final DatanodeConfiguration datanodeConfiguration) {
        this.datanodeConfiguration = datanodeConfiguration;

        this.tranportCertificateFile = localConfiguration.getDatanodeTransportCertificate();
        this.transportCertificatePassword = localConfiguration.getDatanodeTransportCertificatePassword();

        this.httpCertificateFile = localConfiguration.getDatanodeHttpCertificate();
        this.httpCertificatePassword = localConfiguration.getDatanodeHttpCertificatePassword();
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
        final InMemoryKeystoreInformation transportKeystore = reencrypt(new FilesystemKeystoreInformation(transportCertPath, transportCertificatePassword.toCharArray()));

        final Path httpCertPath = datanodeConfiguration.datanodeDirectories().resolveConfigurationSourceFile(httpCertificateFile).orElseThrow(() -> new RuntimeException("This should not happen, certificate expected"));
        final InMemoryKeystoreInformation httpKeystore = reencrypt(new FilesystemKeystoreInformation(httpCertPath, httpCertificatePassword.toCharArray()));

        return new OpensearchCertificates(transportKeystore, httpKeystore);
    }

    @Nonnull
    private static InMemoryKeystoreInformation reencrypt(KeystoreInformation keystoreInformation) {
        try {
            final char[] oneTimePassword = RandomStringUtils.randomAlphabetic(256).toCharArray();
            final KeyStore reencrypted = KeystoreUtils.newStoreCopyContent(keystoreInformation.loadKeystore(), keystoreInformation.password(), oneTimePassword);
            return new InMemoryKeystoreInformation(reencrypted, oneTimePassword);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
