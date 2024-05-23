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
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;
import org.graylog.security.certutil.keystore.storage.KeystoreFileStorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static org.graylog.datanode.Configuration.HTTP_CERTIFICATE_PASSWORD_PROPERTY;
import static org.graylog.datanode.Configuration.TRANSPORT_CERTIFICATE_PASSWORD_PROPERTY;

public final class UploadedCertFilesSecureConfiguration extends SecureConfiguration {

    private final String uploadedTransportKeystoreFileName;
    private final String uploadedHttpKeystoreFileName;
    private final String datanodeTransportCertificatePassword;
    private final String datanodeHttpCertificatePassword;
    private final KeystoreFileStorage keystoreFileStorage;
    private final DatanodeConfiguration datanodeConfiguration;

    @Inject
    public UploadedCertFilesSecureConfiguration(final Configuration localConfiguration,
                                                final DatanodeConfiguration datanodeConfiguration,
                                                KeystoreFileStorage keystoreFileStorage) {
        super(datanodeConfiguration);
        this.datanodeConfiguration = datanodeConfiguration;
        this.keystoreFileStorage = keystoreFileStorage;

        this.uploadedTransportKeystoreFileName = localConfiguration.getDatanodeTransportCertificate();
        this.uploadedHttpKeystoreFileName = localConfiguration.getDatanodeHttpCertificate();

        this.datanodeTransportCertificatePassword = localConfiguration.getDatanodeTransportCertificatePassword();
        this.datanodeHttpCertificatePassword = localConfiguration.getDatanodeHttpCertificatePassword();
    }

    @Override
    public boolean isConfigured(Configuration localConfiguration) throws OpensearchConfigurationException {

        if (noneOfRequiredConfigOptionsProvided()) {
            return false; // none of the uploaded cert options is provided => not usable for this security config, skip this config
        }

        List<String> errors = new LinkedList<>();

        if (isBlank(datanodeTransportCertificatePassword)) {
            errors.add(TRANSPORT_CERTIFICATE_PASSWORD_PROPERTY + " required. Please configure password to your transport certificates keystore.");
        }

        if (!fileExists(uploadedTransportKeystoreFileName)) {
            errors.add("transport_certificate required. Please provide a path to a certificate file in your configuration.");
        }

        if (isBlank(datanodeHttpCertificatePassword)) {
            errors.add(HTTP_CERTIFICATE_PASSWORD_PROPERTY + " required. Please configure password to your http certificates keystore.");
        }

        if (!fileExists(uploadedHttpKeystoreFileName)) {
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
        return isBlank(datanodeTransportCertificatePassword) &&
                isBlank(datanodeHttpCertificatePassword) &&
                isBlank(uploadedHttpKeystoreFileName) &&
                isBlank(uploadedTransportKeystoreFileName);
    }

    @Override
    public OpensearchSecurityConfiguration build() throws KeyStoreStorageException, IOException, GeneralSecurityException {

        final Path targetTransportKeystoreLocation = getTransportKeystoreLocation();
        final Path targetHttpKeystoreLocation = getHttpKeystoreLocation();

        final char[] transportOTP = reEncyptWithOtp(datanodeConfiguration.datanodeDirectories().resolveConfigurationSourceFile(uploadedTransportKeystoreFileName).orElseThrow(() -> new RuntimeException("This should not happen, certificate expected")),
                datanodeTransportCertificatePassword.toCharArray(),
                targetTransportKeystoreLocation);

        final char[] httpOTP = reEncyptWithOtp(datanodeConfiguration.datanodeDirectories().resolveConfigurationSourceFile(uploadedHttpKeystoreFileName).orElseThrow(() -> new RuntimeException("This should not happen, certificate expected")),
                datanodeHttpCertificatePassword.toCharArray(),
                targetHttpKeystoreLocation);

        return new OpensearchSecurityConfiguration(
                new KeystoreInformation(targetTransportKeystoreLocation.toAbsolutePath(), transportOTP),
                new KeystoreInformation(targetHttpKeystoreLocation.toAbsolutePath(), httpOTP)
        );
    }

    public char[] reEncyptWithOtp(final Path originalLocation,
                                  final char[] originalPassword,
                                  final Path targetLocation
    ) throws KeyStoreStorageException {
        // caution! This password changes during each configuration request!!!
        final char[] oneTimePassword = RandomStringUtils.randomAlphabetic(256).toCharArray();
        reEncypt(originalLocation, originalPassword, targetLocation, oneTimePassword);
        return oneTimePassword;
    }

    private void reEncypt(final Path originalLocation,
                          final char[] originalPassword,
                          final Path targetLocation,
                          final char[] newPassword
    ) throws KeyStoreStorageException {

        final Optional<KeyStore> keyStore = keystoreFileStorage.readKeyStore(originalLocation, originalPassword);
        if (keyStore.isPresent()) {
            final KeyStore originalKeystore = keyStore.get();
            keystoreFileStorage.writeKeyStore(targetLocation, originalKeystore, originalPassword, newPassword);
        } else {
            throw new KeyStoreStorageException("No keystore present in : " + originalLocation);
        }
    }
}
