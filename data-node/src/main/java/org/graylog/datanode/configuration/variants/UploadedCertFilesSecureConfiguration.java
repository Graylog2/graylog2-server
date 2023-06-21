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

import com.google.common.collect.ImmutableMap;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.configuration.OpensearchConfigurationException;
import org.graylog.datanode.configuration.RootCertificateFinder;
import org.graylog.datanode.configuration.TlsConfigurationSupplier;
import org.graylog.datanode.configuration.TruststoreCreator;
import org.graylog.datanode.configuration.certificates.CertificateMetaData;
import org.graylog.datanode.configuration.certificates.KeystoreReEncryption;
import org.graylog.datanode.configuration.verification.ConfigProperty;
import org.graylog.datanode.configuration.verification.ConfigSectionCompleteness;
import org.graylog.datanode.configuration.verification.ConfigSectionCompletenessVerifier;
import org.graylog.datanode.configuration.verification.ConfigSectionRequirements;
import org.graylog.security.certutil.CertConstants;
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;
import org.graylog.security.certutil.keystore.storage.location.KeystoreFileLocation;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.graylog.datanode.Configuration.HTTP_CERTIFICATE_PASSWORD_PROPERTY;
import static org.graylog.datanode.Configuration.TRANSPORT_CERTIFICATE_PASSWORD_PROPERTY;
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
    private final ConfigSectionCompletenessVerifier configSectionCompletenessVerifier;

    @Inject
    public UploadedCertFilesSecureConfiguration(final Configuration localConfiguration,
                                                final KeystoreReEncryption keystoreReEncryption,
                                                final TlsConfigurationSupplier tlsConfigurationSupplier,
                                                final TruststoreCreator truststoreCreator,
                                                final RootCertificateFinder rootCertificateFinder,
                                                final ConfigSectionCompletenessVerifier configSectionCompletenessVerifier) {
        super(localConfiguration);
        this.keystoreReEncryption = keystoreReEncryption;
        this.tlsConfigurationSupplier = tlsConfigurationSupplier;
        this.truststoreCreator = truststoreCreator;
        this.rootCertificateFinder = rootCertificateFinder;
        this.configSectionCompletenessVerifier = configSectionCompletenessVerifier;
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
        final ConfigSectionRequirements configSectionRequirements = new ConfigSectionRequirements(
                List.of(new ConfigProperty(TRANSPORT_CERTIFICATE_PASSWORD_PROPERTY, datanodeTransportCertificatePassword),
                        new ConfigProperty(HTTP_CERTIFICATE_PASSWORD_PROPERTY, datanodeHttpCertificatePassword)),
                Arrays.asList(uploadedTransportKeystorePath,
                        uploadedHttpKeystorePath)
        );
        final ConfigSectionCompleteness configSectionCompleteness = configSectionCompletenessVerifier.verifyConfigSectionCompleteness(configSectionRequirements);
        return switch (configSectionCompleteness) {
            case INCOMPLETE -> throw new OpensearchConfigurationException("Configuration incomplete, check the following settings: " + configSectionRequirements.requirementsList());
            case COMPLETE -> true;
            case MISSING -> false;
        };
    }

    @Override
    public Map<String, String> configure(Configuration localConfiguration) throws KeyStoreStorageException, IOException, GeneralSecurityException {
        final ImmutableMap.Builder<String, String> config = ImmutableMap.builder();
        config.putAll(commonSecureConfig(localConfiguration));
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
        config.putAll(tlsConfigurationSupplier.getTransportTlsConfig(new CertificateMetaData(
                        localConfiguration.getDatanodeTransportCertificate(),
                        transportOTP
                )
        ));

        rootCerts.put("http-chain-CA-root", rootCertificateFinder.findRootCert(
                finalHttpKeystoreLocation.keystorePath(),
                httpOTP,
                CertConstants.DATANODE_KEY_ALIAS));
        config.putAll(tlsConfigurationSupplier.getHttpTlsConfig(new CertificateMetaData(
                        localConfiguration.getDatanodeHttpCertificate(),
                        httpOTP
                )
        ));

        if (!rootCerts.isEmpty()) {
            final Path trustStorePath = opensearchConfigDir.resolve(TRUSTSTORE_FILENAME);
            truststoreCreator.createTruststore(rootCerts,
                    truststorePassword.toCharArray(),
                    trustStorePath
            );
            System.setProperty("javax.net.ssl.trustStore", trustStorePath.toAbsolutePath().toString());
            System.setProperty("javax.net.ssl.trustStorePassword", truststorePassword);
            config.putAll(tlsConfigurationSupplier.getTrustStoreTlsConfig(truststorePassword));
        }

        return config.build();
    }
}
