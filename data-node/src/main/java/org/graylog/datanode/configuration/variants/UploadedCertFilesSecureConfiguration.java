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
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.datanode.configuration.OpensearchConfigurationException;
import org.graylog.datanode.configuration.certificates.KeystoreReEncryption;
import org.graylog.datanode.configuration.verification.ConfigProperty;
import org.graylog.datanode.configuration.verification.ConfigSectionCompleteness;
import org.graylog.datanode.configuration.verification.ConfigSectionCompletenessVerifier;
import org.graylog.datanode.configuration.verification.ConfigSectionRequirements;
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;
import org.graylog.security.certutil.keystore.storage.location.KeystoreFileLocation;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

import static org.graylog.datanode.Configuration.HTTP_CERTIFICATE_PASSWORD_PROPERTY;
import static org.graylog.datanode.Configuration.TRANSPORT_CERTIFICATE_PASSWORD_PROPERTY;

public final class UploadedCertFilesSecureConfiguration extends SecureConfiguration {

    private final Path uploadedTransportKeystorePath;
    private final Path uploadedHttpKeystorePath;
    private final KeystoreFileLocation finalTransportKeystoreLocation;
    private final KeystoreFileLocation finalHttpKeystoreLocation;
    private final String datanodeTransportCertificatePassword;
    private final String datanodeHttpCertificatePassword;
    private final KeystoreReEncryption keystoreReEncryption;
    private final ConfigSectionCompletenessVerifier configSectionCompletenessVerifier;

    @Inject
    public UploadedCertFilesSecureConfiguration(final Configuration localConfiguration,
                                                final DatanodeConfiguration datanodeConfiguration,
                                                final KeystoreReEncryption keystoreReEncryption,
                                                final ConfigSectionCompletenessVerifier configSectionCompletenessVerifier) {
        super(datanodeConfiguration);
        this.keystoreReEncryption = keystoreReEncryption;
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
    public OpensearchSecurityConfiguration build() throws KeyStoreStorageException, IOException, GeneralSecurityException {

        final char[] transportOTP = keystoreReEncryption.reEncyptWithOtp(new KeystoreFileLocation(uploadedTransportKeystorePath),
                datanodeTransportCertificatePassword.toCharArray(),
                finalTransportKeystoreLocation);

        final char[] httpOTP = keystoreReEncryption.reEncyptWithOtp(new KeystoreFileLocation(uploadedHttpKeystorePath),
                datanodeHttpCertificatePassword.toCharArray(),
                finalHttpKeystoreLocation);

        return new OpensearchSecurityConfiguration(
                new KeystoreInformation(finalTransportKeystoreLocation.keystorePath().toAbsolutePath(), transportOTP),
                new KeystoreInformation(finalHttpKeystoreLocation.keystorePath().toAbsolutePath(), httpOTP)
        );
    }
}
