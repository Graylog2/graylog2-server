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
import org.graylog.datanode.Configuration;
import org.graylog.datanode.configuration.variants.InSecureConfiguration;
import org.graylog.datanode.configuration.variants.MongoCertSecureConfiguration;
import org.graylog.datanode.configuration.variants.OpensearchSecurityConfiguration;
import org.graylog.datanode.configuration.variants.SecurityConfigurationVariant;
import org.graylog.datanode.configuration.variants.UploadedCertFilesSecureConfiguration;
import org.graylog.datanode.process.OpensearchConfiguration;
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;
import org.graylog2.bootstrap.preflight.PreflightConfigResult;
import org.graylog2.bootstrap.preflight.PreflightConfigService;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.NodeService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
public class OpensearchConfigurationProvider implements Provider<OpensearchConfiguration> {
    private final Configuration localConfiguration;
    private final UploadedCertFilesSecureConfiguration uploadedCertFilesSecureConfiguration;
    private final MongoCertSecureConfiguration mongoCertSecureConfiguration;
    private final InSecureConfiguration inSecureConfiguration;
    private final DatanodeConfiguration datanodeConfiguration;
    private final byte[] signingKey;
    private final NodeService<DataNodeDto> nodeService;
    private final PreflightConfigService preflightConfigService;

    @Inject
    public OpensearchConfigurationProvider(final Configuration localConfiguration,
                                           final DatanodeConfiguration datanodeConfiguration,
                                           final UploadedCertFilesSecureConfiguration uploadedCertFilesSecureConfiguration,
                                           final MongoCertSecureConfiguration mongoCertSecureConfiguration,
                                           final InSecureConfiguration inSecureConfiguration,
                                           final NodeService<DataNodeDto> nodeService,
                                           final PreflightConfigService preflightConfigService,
                                           final @Named("password_secret") String passwordSecret) {
        this.localConfiguration = localConfiguration;
        this.datanodeConfiguration = datanodeConfiguration;
        this.uploadedCertFilesSecureConfiguration = uploadedCertFilesSecureConfiguration;
        this.mongoCertSecureConfiguration = mongoCertSecureConfiguration;
        this.inSecureConfiguration = inSecureConfiguration;
        this.signingKey = passwordSecret.getBytes(StandardCharsets.UTF_8);
        this.nodeService = nodeService;
        this.preflightConfigService = preflightConfigService;
    }

    private boolean isPreflight() {
        final PreflightConfigResult preflightResult = preflightConfigService.getPreflightConfigResult();

        // if preflight is finished, we assume that there will be some datanode registered via node-service.
        return preflightResult != PreflightConfigResult.FINISHED;
    }

    @Override
    public OpensearchConfiguration get() {
        //TODO: at some point bind the whole list, for now there is too much experiments with order and prerequisites
        List<SecurityConfigurationVariant> securityConfigurationTypes = List.of(
                inSecureConfiguration,
                uploadedCertFilesSecureConfiguration,
                mongoCertSecureConfiguration
        );

        Optional<SecurityConfigurationVariant> chosenSecurityConfigurationVariant = securityConfigurationTypes.stream()
                .filter(s -> s.isConfigured(localConfiguration))
                .findFirst();

        try {
            ImmutableMap.Builder<String, String> opensearchProperties = ImmutableMap.builder();

            if (localConfiguration.getInitialManagerNodes() != null && !localConfiguration.getInitialManagerNodes().isBlank()) {
                opensearchProperties.put("cluster.initial_master_nodes", localConfiguration.getInitialManagerNodes());
            } else if (isPreflight()) {
                final var nodeList = String.join(",", nodeService.allActive().values().stream().map(Node::getHostname).collect(Collectors.toSet()));
                opensearchProperties.put("cluster.initial_master_nodes", nodeList);
            }
            opensearchProperties.putAll(commonOpensearchConfig(localConfiguration));

            OpensearchSecurityConfiguration securityConfiguration = null;
            if (chosenSecurityConfigurationVariant.isPresent()) {
                securityConfiguration = chosenSecurityConfigurationVariant.get()
                        .build()
                        .configure(datanodeConfiguration, signingKey);
                opensearchProperties.putAll(securityConfiguration.getProperties());
            }

            return new OpensearchConfiguration(
                    datanodeConfiguration.opensearchDistributionProvider().get(),
                    datanodeConfiguration.datanodeDirectories(),
                    localConfiguration.getBindAddress(),
                    localConfiguration.getHostname(),
                    localConfiguration.getOpensearchHttpPort(),
                    localConfiguration.getOpensearchTransportPort(),
                    localConfiguration.getClustername(),
                    localConfiguration.getDatanodeNodeName(),
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
        localConfiguration.getOpensearchNetworkHost().ifPresent(
                networkHost -> config.put("network.host", networkHost));
        config.put("path.data", datanodeConfiguration.datanodeDirectories().getDataTargetDir().toString());
        config.put("path.logs", datanodeConfiguration.datanodeDirectories().getLogsTargetDir().toString());

        config.put("network.bind_host", localConfiguration.getBindAddress());
        //config.put("network.publish_host", Tools.getLocalCanonicalHostname());

        // Uncomment the following line to get DEBUG logs for the underlying Opensearch
        //config.put("logger.org.opensearch", "debug");

        return config.build();
    }

}
