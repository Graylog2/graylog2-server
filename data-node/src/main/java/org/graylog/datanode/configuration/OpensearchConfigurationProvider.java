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
import org.graylog2.bootstrap.preflight.PreflightConfig;
import org.graylog2.bootstrap.preflight.PreflightConfigResult;
import org.graylog2.bootstrap.preflight.PreflightConfigService;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
public class OpensearchConfigurationProvider implements Provider<OpensearchConfiguration> {
    private final int MAX_TRIES = 60;
    private final Configuration localConfiguration;
    private final UploadedCertFilesSecureConfiguration uploadedCertFilesSecureConfiguration;
    private final MongoCertSecureConfiguration mongoCertSecureConfiguration;
    private final InSecureConfiguration inSecureConfiguration;
    private final DatanodeConfiguration datanodeConfiguration;
    private final byte[] signingKey;
    private final String nodeName;
    private final NodeService nodeService;
    private final PreflightConfigService preflightConfigService;

    @Inject
    public OpensearchConfigurationProvider(final Configuration localConfiguration,
                                           final DatanodeConfiguration datanodeConfiguration,
                                           final UploadedCertFilesSecureConfiguration uploadedCertFilesSecureConfiguration,
                                           final MongoCertSecureConfiguration mongoCertSecureConfiguration,
                                           final InSecureConfiguration inSecureConfiguration,
                                           final NodeService nodeService,
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
        this.nodeName = DatanodeConfigurationProvider.getNodesFromConfig(localConfiguration.getDatanodeNodeName());
    }

    private boolean isPreflight() {
        final PreflightConfigResult preflightResult = preflightConfigService.getPreflightConfigResult();

        // if preflight is finished, we assume that there will be some datanode registered via node-service.
        return preflightResult != PreflightConfigResult.FINISHED;
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

            if(localConfiguration.getInitialManagerNodes() != null && !localConfiguration.getInitialManagerNodes().isBlank()) {
                opensearchProperties.put("cluster.initial_master_nodes", localConfiguration.getInitialManagerNodes());
            } else if(isPreflight()) {
                var maxCounter = new AtomicInteger(0);
                try {
                    var activeNodes = nodeService.allActive(Node.Type.DATANODE);
                    // at this point, there should at least be the current node in the list of active nodes. If not, then we have a race condition with the
                    // registration of the node. So we sleep for a second and try again.
                    while (activeNodes.isEmpty()) {
                        Thread.sleep(1000);
                        activeNodes = nodeService.allActive(Node.Type.DATANODE);
                        if(maxCounter.getAndIncrement() > MAX_TRIES) {
                            throw new OpensearchConfigurationException("No active nodes found. Aborting DataNode configuration.");
                        }
                    }
                    final var nodeList = String.join(",", activeNodes.values().stream().map(Node::getHostname).toList());
                    opensearchProperties.put("cluster.initial_master_nodes", nodeList);
                } catch (InterruptedException e) {
                    throw new OpensearchConfigurationException(e);
                }
            }
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
                    localConfiguration.getBindAddress(),
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

        config.put("network.bind_host", localConfiguration.getBindAddress());
        //config.put("network.publish_host", Tools.getLocalCanonicalHostname());

        // Uncomment the following line to get DEBUG logs for the underlying Opensearch
        //config.put("logger.org.opensearch", "debug");

        return config.build();
    }

}
