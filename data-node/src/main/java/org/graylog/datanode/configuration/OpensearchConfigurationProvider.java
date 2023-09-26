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

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.configuration.variants.InSecureConfiguration;
import org.graylog.datanode.configuration.variants.MongoCertSecureConfiguration;
import org.graylog.datanode.configuration.variants.OpensearchSecurityConfiguration;
import org.graylog.datanode.configuration.variants.SecurityConfigurationVariant;
import org.graylog.datanode.configuration.variants.UploadedCertFilesSecureConfiguration;
import org.graylog.datanode.process.OpensearchConfiguration;
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Singleton
public class OpensearchConfigurationProvider implements Provider<OpensearchConfiguration> {
    private final Configuration localConfiguration;
    private final UploadedCertFilesSecureConfiguration uploadedCertFilesSecureConfiguration;
    private final MongoCertSecureConfiguration mongoCertSecureConfiguration;
    private final InSecureConfiguration inSecureConfiguration;
    private final DatanodeConfiguration datanodeConfiguration;
    private final byte[] signingKey;
    private final String nodeName;

    @Inject
    public OpensearchConfigurationProvider(Configuration localConfiguration,
                                           DatanodeConfiguration datanodeConfiguration,
                                           UploadedCertFilesSecureConfiguration uploadedCertFilesSecureConfiguration,
                                           MongoCertSecureConfiguration mongoCertSecureConfiguration,
                                           InSecureConfiguration inSecureConfiguration,
                                           final @Named("password_secret") String passwordSecret) {
        this.localConfiguration = localConfiguration;
        this.datanodeConfiguration = datanodeConfiguration;
        this.uploadedCertFilesSecureConfiguration = uploadedCertFilesSecureConfiguration;
        this.mongoCertSecureConfiguration = mongoCertSecureConfiguration;
        this.inSecureConfiguration = inSecureConfiguration;
        this.signingKey = passwordSecret.getBytes(StandardCharsets.UTF_8);
        this.nodeName = DatanodeConfigurationProvider.getNodesFromConfig(localConfiguration.getDatanodeNodeName());
    }

    @Override
    public OpensearchConfiguration get() {
        final Path opensearchConfigLocation = Path.of(localConfiguration.getOpensearchConfigLocation());

        //TODO: at some point bind the whole list, for now there is too much experiments with order and prerequisites
        List<SecurityConfigurationVariant> securityConfigurationTypes = List.of(
                inSecureConfiguration,
                uploadedCertFilesSecureConfiguration,
                mongoCertSecureConfiguration
        );

        Optional<SecurityConfigurationVariant> chosenSecurityConfigurationVariant = securityConfigurationTypes.stream()
                .filter(s -> s.checkPrerequisites(localConfiguration))
                .findFirst();

        try {
            ImmutableMap.Builder<String, String> opensearchProperties = ImmutableMap.builder();
            opensearchProperties.putAll(commonOpensearchConfig(localConfiguration));
            OpensearchSecurityConfiguration securityConfiguration = null;
            if (chosenSecurityConfigurationVariant.isPresent()) {
                securityConfiguration = chosenSecurityConfigurationVariant.get()
                        .build()
                        .configure(localConfiguration, signingKey);
                opensearchProperties.putAll(securityConfiguration.getProperties());
            }

            return new OpensearchConfiguration(
                    datanodeConfiguration.opensearchDistribution().directory(),
                    opensearchConfigLocation,
                    localConfiguration.getHttpBindAddress().getHost(),
                    localConfiguration.getHostname(),
                    localConfiguration.getOpensearchHttpPort(),
                    localConfiguration.getOpensearchTransportPort(),
                    "datanode-cluster",
                    nodeName,
                    List.of(),
                    localConfiguration.getOpensearchDiscoverySeedHosts(),
                    securityConfiguration,
                    opensearchProperties.build()
            );
        } catch (GeneralSecurityException | KeyStoreStorageException | IOException e) {
            throw new OpensearchConfigurationException(e);
        }
    }

    private ImmutableMap<String, String> commonOpensearchConfig(final Configuration localConfiguration) {
        final ImmutableMap.Builder<String, String> config = ImmutableMap.builder();
        Objects.requireNonNull(localConfiguration.getConfigLocation(), "config_location setting is required!");
        localConfiguration.getOpensearchNetworkHostHost().ifPresent(
                networkHost -> config.put("network.host", networkHost));
        config.put("path.data", Path.of(localConfiguration.getOpensearchDataLocation()).resolve(nodeName).toAbsolutePath().toString());
        config.put("path.logs", Path.of(localConfiguration.getOpensearchLogsLocation()).resolve(nodeName).toAbsolutePath().toString());

        if(StringUtils.isNotBlank(localConfiguration.getInitialManagerNodes())) {
            config.put("cluster.initial_master_nodes", localConfiguration.getInitialManagerNodes());
        }

        // listen on all interfaces
        config.put("network.bind_host", "0.0.0.0");
        //config.put("network.publish_host", Tools.getLocalCanonicalHostname());

        // Uncomment the following line to get DEBUG logs for the underlying Opensearch
        //config.put("logger.org.opensearch", "debug");

        return config.build();
    }

}
