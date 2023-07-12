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

import org.graylog.datanode.Configuration;
import org.graylog.datanode.configuration.variants.InSecureConfiguration;
import org.graylog.datanode.configuration.variants.MongoCertSecureConfiguration;
import org.graylog.datanode.configuration.variants.SecurityConfigurationVariant;
import org.graylog.datanode.configuration.variants.UploadedCertFilesSecureConfiguration;
import org.graylog.datanode.process.OpensearchConfiguration;
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;
import org.graylog2.plugin.Tools;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Singleton
public class OpensearchConfigurationProvider implements Provider<OpensearchConfiguration> {


    private final Configuration localConfiguration;
    private final UploadedCertFilesSecureConfiguration uploadedCertFilesSecureConfiguration;
    private final MongoCertSecureConfiguration mongoCertSecureConfiguration;
    private final InSecureConfiguration inSecureConfiguration;
    private final DatanodeConfiguration datanodeConfiguration;

    @Inject
    public OpensearchConfigurationProvider(Configuration localConfiguration,
                                           DatanodeConfiguration datanodeConfiguration,
                                           UploadedCertFilesSecureConfiguration uploadedCertFilesSecureConfiguration,
                                           MongoCertSecureConfiguration mongoCertSecureConfiguration,
                                           InSecureConfiguration inSecureConfiguration) {
        this.localConfiguration = localConfiguration;
        this.datanodeConfiguration = datanodeConfiguration;
        this.uploadedCertFilesSecureConfiguration = uploadedCertFilesSecureConfiguration;
        this.mongoCertSecureConfiguration = mongoCertSecureConfiguration;
        this.inSecureConfiguration = inSecureConfiguration;
    }

    @Override
    public OpensearchConfiguration get() {
        final String opensearchConfigLocation = localConfiguration.getOpensearchConfigLocation();

        //TODO: at some point bind the whole list, for now there is too much experiments with order and prerequisites
        List<SecurityConfigurationVariant> securityConfigurationTypes = List.of(
                uploadedCertFilesSecureConfiguration,
                mongoCertSecureConfiguration,
                inSecureConfiguration //TODO: in final version, this configuration is tried first, not last
        );

        SecurityConfigurationVariant chosenSecurityConfigurationVariant = securityConfigurationTypes.stream()
                .filter(s -> s.checkPrerequisites(localConfiguration))
                .findFirst()
                .orElseThrow(() -> new OpensearchConfigurationException("No valid option to start up OpenSearch"));

        try {
            final Map<String, String> config = chosenSecurityConfigurationVariant.configure(localConfiguration);

            return new OpensearchConfiguration(
                    datanodeConfiguration.opensearchDistribution().directory(),
                    Path.of(opensearchConfigLocation),
                    localConfiguration.getHttpBindAddress().getHost(),
                    localConfiguration.getOpensearchHttpPort(),
                    localConfiguration.getOpensearchTransportPort(),
                    localConfiguration.getRestApiUsername(),
                    localConfiguration.getRestApiPassword(),
                    "datanode-cluster",
                    localConfiguration.getDatanodeNodeName(),
                    Collections.emptyList(),
                    localConfiguration.getOpensearchDiscoverySeedHosts(),
                    config
            );
        } catch (GeneralSecurityException | KeyStoreStorageException | IOException e) {
            throw new OpensearchConfigurationException(e);
        }
    }

}
