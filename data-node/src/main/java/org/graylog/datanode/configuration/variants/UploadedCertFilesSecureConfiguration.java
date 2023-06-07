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

import org.graylog.datanode.Configuration;
import org.graylog.datanode.configuration.RootCertificateFinder;
import org.graylog.datanode.configuration.TlsConfigurationSupplier;
import org.graylog.datanode.configuration.TruststoreCreator;
import org.graylog.datanode.configuration.certificates.CertificateMetaData;
import org.graylog.datanode.configuration.certificates.KeystoreReEncryption;
import org.graylog.security.certutil.CertConstants;
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;
import org.graylog.security.certutil.keystore.storage.location.KeystoreFileLocation;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.graylog.datanode.configuration.TlsConfigurationSupplier.TRUSTSTORE_FILENAME;

public final class UploadedCertFilesSecureConfiguration extends SecureConfiguration {

    private final Path uploadedTransportKeystorePath;
    private final Path uploadedHttpKeystorePath;
    private final KeystoreFileLocation finalTransportKeystoreLocation;
    private final KeystoreFileLocation finalHttpKeystoreLocation;
    private final String datanodeTransportCertificatePassword;
    private final String datanodeHttpCertificatePassword;
    private final KeystoreReEncryption keystoreReEncryption;
    private final TlsConfigurationSupplier tlsConfigurationSupplier;
    private final TruststoreCreator truststoreCreator;
    private final RootCertificateFinder rootCertificateFinder;

    @Inject
    public UploadedCertFilesSecureConfiguration(final Configuration localConfiguration,
                                                final KeystoreReEncryption keystoreReEncryption,
                                                final TlsConfigurationSupplier tlsConfigurationSupplier,
                                                final TruststoreCreator truststoreCreator,
                                                final RootCertificateFinder rootCertificateFinder) {
        super(localConfiguration);
        this.keystoreReEncryption = keystoreReEncryption;
        this.tlsConfigurationSupplier = tlsConfigurationSupplier;
        this.truststoreCreator = truststoreCreator;
        this.rootCertificateFinder = rootCertificateFinder;
        this.uploadedTransportKeystorePath = datanodeConfigDir
                .resolve(localConfiguration.getDatanodeTransportCertificate());
        this.uploadedHttpKeystorePath = datanodeConfigDir
                .resolve(localConfiguration.getDatanodeHttpCertificate());


        this.finalTransportKeystoreLocation = new KeystoreFileLocation(
                opensearchConfigDir.resolve(localConfiguration.getDatanodeTransportCertificate())
        );
        this.finalHttpKeystoreLocation = new KeystoreFileLocation(
                opensearchConfigDir.resolve(localConfiguration.getDatanodeHttpCertificate())
        );

        this.datanodeTransportCertificatePassword = localConfiguration.getDatanodeTransportCertificatePassword();
        this.datanodeHttpCertificatePassword = localConfiguration.getDatanodeHttpCertificatePassword();
    }

    @Override
    public boolean checkPrerequisites(Configuration localConfiguration) {
        final boolean bothCertFilesPresent = Files.exists(uploadedTransportKeystorePath) && Files.exists(uploadedHttpKeystorePath);
        final boolean bothCertPasswordsPresent = datanodeTransportCertificatePassword != null && datanodeHttpCertificatePassword != null;
        return bothCertFilesPresent && bothCertPasswordsPresent;
    }

    @Override
    public Map<String, String> configure(Configuration localConfiguration) throws KeyStoreStorageException, IOException, GeneralSecurityException {
        Map<String, String> config = commonSecureConfig(localConfiguration);
        Map<String, X509Certificate> rootCerts = new HashMap<>();
        final String truststorePassword = UUID.randomUUID().toString();
        final char[] transportOTP = keystoreReEncryption.reEncyptWithOtp(new KeystoreFileLocation(uploadedTransportKeystorePath),
                datanodeTransportCertificatePassword.toCharArray(),
                finalTransportKeystoreLocation);
        final char[] httpOTP = keystoreReEncryption.reEncyptWithOtp(new KeystoreFileLocation(uploadedHttpKeystorePath),
                datanodeHttpCertificatePassword.toCharArray(),
                finalHttpKeystoreLocation);

        configureInitialAdmin(localConfiguration, localConfiguration.getRestApiUsername(), localConfiguration.getRestApiPassword());

        rootCerts.put("transport-chain-CA-root", rootCertificateFinder.findRootCert(
                finalTransportKeystoreLocation.keystorePath(),
                transportOTP,
                CertConstants.DATANODE_KEY_ALIAS));
        tlsConfigurationSupplier.addTransportTlsConfig(config,
                new CertificateMetaData(
                        localConfiguration.getDatanodeTransportCertificate(),
                        transportOTP
                )
        );

        rootCerts.put("http-chain-CA-root", rootCertificateFinder.findRootCert(
                finalHttpKeystoreLocation.keystorePath(),
                httpOTP,
                CertConstants.DATANODE_KEY_ALIAS));
        tlsConfigurationSupplier.addHttpTlsConfig(config,
                new CertificateMetaData(
                        localConfiguration.getDatanodeHttpCertificate(),
                        httpOTP
                )
        );

        if (!rootCerts.isEmpty()) {
            final Path trustStorePath = opensearchConfigDir.resolve(TRUSTSTORE_FILENAME);
            truststoreCreator.createTruststore(rootCerts,
                    truststorePassword.toCharArray(),
                    trustStorePath
            );
            System.setProperty("javax.net.ssl.trustStore", trustStorePath.toAbsolutePath().toString());
            System.setProperty("javax.net.ssl.trustStorePassword", truststorePassword);
            tlsConfigurationSupplier.addTrustStoreTlsConfig(config, truststorePassword);
        }

        return config;
    }
}
