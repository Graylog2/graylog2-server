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
import org.graylog.datanode.OpensearchDistribution;
import org.graylog.datanode.configuration.variants.InSecureConfiguration;
import org.graylog.datanode.configuration.variants.MongoCertSecureConfiguration;
import org.graylog.datanode.configuration.variants.SecurityConfigurationVariant;
import org.graylog.datanode.configuration.variants.UploadedCertFilesSecureConfiguration;
import org.graylog.datanode.process.OpensearchConfiguration;
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;

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
public class ConfigurationProvider implements Provider<OpensearchConfiguration> {

    private final OpensearchConfiguration configuration;

    @Inject
    public ConfigurationProvider(Configuration localConfiguration,
                                 UploadedCertFilesSecureConfiguration uploadedCertFilesSecureConfiguration,
                                 MongoCertSecureConfiguration mongoCertSecureConfiguration,
                                 InSecureConfiguration inSecureConfiguration,
                                 DataNodeConfig sharedConfiguration,
                                 OpensearchDistribution opensearchDistribution
    ) throws IOException, GeneralSecurityException, KeyStoreStorageException {
        final var cfg = sharedConfiguration.test();
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
                .orElseThrow(() -> new RuntimeException("No valid option to start up OpenSearch"));

        final Map<String, String> config = chosenSecurityConfigurationVariant.configure(localConfiguration);
        configuration = new OpensearchConfiguration(
                opensearchDistribution.version(),
                opensearchDistribution.directory(),
                Path.of(opensearchConfigLocation),
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
    }

    @Override
    public OpensearchConfiguration get() {
        return configuration;
    }

}
