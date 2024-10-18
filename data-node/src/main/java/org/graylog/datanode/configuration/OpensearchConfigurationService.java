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
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractIdleService;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.configuration.variants.InSecureConfiguration;
import org.graylog.datanode.configuration.variants.LocalKeystoreSecureConfiguration;
import org.graylog.datanode.configuration.variants.OpensearchSecurityConfiguration;
import org.graylog.datanode.configuration.variants.SecurityConfigurationVariant;
import org.graylog.datanode.configuration.variants.UploadedCertFilesSecureConfiguration;
import org.graylog.datanode.opensearch.OpensearchConfigurationChangeEvent;
import org.graylog.datanode.opensearch.configuration.OpensearchConfiguration;
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.security.JwtSecret;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Singleton
public class OpensearchConfigurationService extends AbstractIdleService {
    private final Configuration localConfiguration;
    private final UploadedCertFilesSecureConfiguration uploadedCertFilesSecureConfiguration;
    private final LocalKeystoreSecureConfiguration localKeystoreSecureConfiguration;
    private final InSecureConfiguration inSecureConfiguration;
    private final DatanodeConfiguration datanodeConfiguration;
    private final JwtSecret signingKey;
    private final NodeService<DataNodeDto> nodeService;
    private final S3RepositoryConfiguration s3RepositoryConfiguration;

    /**
     * This configuration won't survive datanode restart. But it can be repeatedly provided to the managed opensearch
     */
    private final Map<String, Object> transientConfiguration = new ConcurrentHashMap<>();

    private final List<X509Certificate> trustedCertificates = new ArrayList<>();
    private final EventBus eventBus;

    @Inject
    public OpensearchConfigurationService(final Configuration localConfiguration,
                                          final DatanodeConfiguration datanodeConfiguration,
                                          final UploadedCertFilesSecureConfiguration uploadedCertFilesSecureConfiguration,
                                          final LocalKeystoreSecureConfiguration localKeystoreSecureConfiguration,
                                          final InSecureConfiguration inSecureConfiguration,
                                          final NodeService<DataNodeDto> nodeService,
                                          JwtSecret jwtSecret,
                                          final S3RepositoryConfiguration s3RepositoryConfiguration,
                                          final EventBus eventBus) {
        this.localConfiguration = localConfiguration;
        this.datanodeConfiguration = datanodeConfiguration;
        this.uploadedCertFilesSecureConfiguration = uploadedCertFilesSecureConfiguration;
        this.localKeystoreSecureConfiguration = localKeystoreSecureConfiguration;
        this.inSecureConfiguration = inSecureConfiguration;
        this.signingKey = jwtSecret;
        this.nodeService = nodeService;
        this.s3RepositoryConfiguration = s3RepositoryConfiguration;
        this.eventBus = eventBus;
        eventBus.register(this);
    }

    @Override
    protected void startUp() {
        triggerConfigurationChangedEvent();
    }

    @Override
    protected void shutDown() {

    }

    @Subscribe
    public void onKeystoreChange(DatanodeKeystoreChangedEvent event) {
        // configuration relies on the keystore. Every change there should rebuild the configuration and restart
        // dependent services
        triggerConfigurationChangedEvent();
    }


    public void setAllowlist(List<String> allowlist, List<X509Certificate> trustedCertificates) {
        this.trustedCertificates.addAll(trustedCertificates);
        setTransientConfiguration("reindex.remote.allowlist", allowlist);
    }

    public void removeAllowlist() {
        removeTransientConfiguration("reindex.remote.allowlist");
    }

    public void setTransientConfiguration(String key, Object value) {
        this.transientConfiguration.put(key, value);
        triggerConfigurationChangedEvent();
    }

    public void removeTransientConfiguration(String key) {
        final Object removedValue = this.transientConfiguration.remove(key);
        if (removedValue != null) {
            triggerConfigurationChangedEvent();
        }
    }

    private OpensearchConfiguration get() {
        //TODO: at some point bind the whole list, for now there is too much experiments with order and prerequisites
        List<SecurityConfigurationVariant> securityConfigurationTypes = List.of(
                inSecureConfiguration,
                uploadedCertFilesSecureConfiguration,
                localKeystoreSecureConfiguration
        );

        Optional<SecurityConfigurationVariant> chosenSecurityConfigurationVariant = securityConfigurationTypes.stream()
                .filter(s -> s.isConfigured(localConfiguration))
                .findFirst();

        try {
            ImmutableMap.Builder<String, Object> opensearchProperties = ImmutableMap.builder();

            if (localConfiguration.getInitialClusterManagerNodes() != null && !localConfiguration.getInitialClusterManagerNodes().isBlank()) {
                opensearchProperties.put("cluster.initial_cluster_manager_nodes", localConfiguration.getInitialClusterManagerNodes());
            } else {
                final var nodeList = String.join(",", nodeService.allActive().values().stream().map(Node::getHostname).collect(Collectors.toSet()));
                opensearchProperties.put("cluster.initial_cluster_manager_nodes", nodeList);
            }
            opensearchProperties.putAll(commonOpensearchConfig(localConfiguration));

            OpensearchSecurityConfiguration securityConfiguration = null;
            if (chosenSecurityConfigurationVariant.isPresent()) {
                securityConfiguration = chosenSecurityConfigurationVariant.get()
                        .build()
                        .configure(datanodeConfiguration, trustedCertificates, signingKey);
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
                    localConfiguration.getNodeRoles(),
                    localConfiguration.getOpensearchDiscoverySeedHosts(),
                    securityConfiguration,
                    s3RepositoryConfiguration,
                    localConfiguration.getNodeSearchCacheSize(),
                    opensearchProperties.build()
            );
        } catch (GeneralSecurityException | KeyStoreStorageException | IOException e) {
            throw new OpensearchConfigurationException(e);
        }
    }

    private ImmutableMap<String, Object> commonOpensearchConfig(final Configuration localConfiguration) {
        final ImmutableMap.Builder<String, Object> config = ImmutableMap.builder();
        localConfiguration.getOpensearchNetworkHost().ifPresent(
                networkHost -> config.put("network.host", networkHost));
        config.put("path.data", datanodeConfiguration.datanodeDirectories().getDataTargetDir().toString());
        config.put("path.logs", datanodeConfiguration.datanodeDirectories().getLogsTargetDir().toString());

        config.put("network.bind_host", localConfiguration.getBindAddress());

        // https://opensearch.org/docs/latest/tuning-your-cluster/availability-and-recovery/snapshots/snapshot-restore/#shared-file-system
        if (localConfiguration.getPathRepo() != null && !localConfiguration.getPathRepo().isEmpty()) {
            config.put("path.repo", localConfiguration.getPathRepo());
        }

        config.put("network.publish_host", localConfiguration.getHostname());

        if (localConfiguration.getOpensearchDebug() != null && !localConfiguration.getOpensearchDebug().isBlank()) {
            config.put("logger.org.opensearch", localConfiguration.getOpensearchDebug());
        }

        if (localConfiguration.getOpensearchAuditLog() != null && !localConfiguration.getOpensearchAuditLog().isBlank()) {
            config.put("plugins.security.audit.type", localConfiguration.getOpensearchAuditLog());
        }

        // common OpenSearch config parameters from our docs
        config.put("indices.query.bool.max_clause_count", localConfiguration.getIndicesQueryBoolMaxClauseCount().toString());

        // enable admin access via the REST API
        config.put("plugins.security.restapi.admin.enabled", "true");

        config.putAll(transientConfiguration);

        return config.build();
    }

    private void triggerConfigurationChangedEvent() {
        eventBus.post(new OpensearchConfigurationChangeEvent(get()));
    }


}
