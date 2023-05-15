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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.OpensearchDistribution;
import org.graylog.datanode.process.OpensearchConfiguration;
import org.graylog.security.certutil.CertutilCert;
import org.graylog.security.certutil.CertutilHttp;
import org.graylog2.jackson.TypeReferences;
import org.graylog2.security.hashing.BCryptPasswordAlgorithm;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.graylog.datanode.configuration.TlsConfigurationSupplier.TRUSTSTORE_FILENAME;

@Singleton
public class ConfigurationProvider implements Provider<OpensearchConfiguration> {

    private final OpensearchConfiguration configuration;

    public static final String SSL_PREFIX = "plugins.security.ssl.";

    @Inject
    public ConfigurationProvider(Configuration localConfiguration,
                                 DataNodeConfig sharedConfiguration,
                                 OpensearchDistribution opensearchDistribution,
                                 TlsConfigurationSupplier tlsConfigurationSupplier,
                                 TruststoreCreator truststoreCreator,
                                 RootCertificateFinder rootCertificateFinder
    ) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        Map<String, X509Certificate> rootCerts = new HashMap<>();
        final String truststorePassword = UUID.randomUUID().toString();
        final var cfg = sharedConfiguration.test();

        final String opensearchConfigLocation = localConfiguration.getOpensearchConfigLocation();
        Objects.requireNonNull(localConfiguration.getConfigLocation(), "config_location setting is required!");
        final Path datanodeConfigDir = Path.of(localConfiguration.getConfigLocation());
        final Path opensearchConfigDir = Path.of(opensearchConfigLocation).resolve("opensearch");

        final LinkedHashMap<String, String> config = new LinkedHashMap<>();
        config.put("path.data", Path.of(localConfiguration.getOpensearchDataLocation()).resolve(localConfiguration.getDatanodeNodeName()).toAbsolutePath().toString());
        config.put("path.logs", Path.of(localConfiguration.getOpensearchLogsLocation()).resolve(localConfiguration.getDatanodeNodeName()).toAbsolutePath().toString());
        if (localConfiguration.isSingleNodeOnly()) {
            config.put("discovery.type", "single-node");
        } else {
            config.put("cluster.initial_master_nodes", "node1");
        }

        // listen on all interfaces
        config.put("network.bind_host", "0.0.0.0");

        localConfiguration.getOpensearchNetworkHostHost().ifPresent(
                networkHost -> config.put("network.host", networkHost));


        //copy from read-only data node configuration, to read-write configuration directory
        Path transportKeystorePath = datanodeConfigDir
                .resolve(localConfiguration.getDatanodeTransportCertificate());
        if (Files.exists(transportKeystorePath)) {
            transportKeystorePath = Files.copy(transportKeystorePath, opensearchConfigDir.resolve(localConfiguration.getDatanodeTransportCertificate()));
        }
        Path httpKeystorePath = datanodeConfigDir
                .resolve(localConfiguration.getDatanodeHttpCertificate());
        if (Files.exists(httpKeystorePath)) {
            httpKeystorePath = Files.copy(httpKeystorePath, opensearchConfigDir.resolve(localConfiguration.getDatanodeHttpCertificate()));
        }

        if (Files.exists(transportKeystorePath) && Files.exists(httpKeystorePath)) {
            config.put("plugins.security.disabled", "false");
            config.put(SSL_PREFIX + "http.enabled", "true");

            final Path internalUsersFile = opensearchConfigDir.resolve("opensearch-security").resolve("internal_users.yml");

            Objects.requireNonNull(localConfiguration.getRestApiUsername(),
                    "rest_api_username has to be configured the usage of secured Opensearch REST api"
            );

            Objects.requireNonNull(localConfiguration.getRestApiPassword(),
                    "rest_api_password has to be configured the usage of secured Opensearch REST api"
            );

            configureInitialAdmin(internalUsersFile, localConfiguration.getRestApiUsername(), localConfiguration.getRestApiPassword());

        } else {
            config.put("plugins.security.disabled", "true");
            config.put(SSL_PREFIX + "http.enabled", "false");
        }


        if (Files.exists(transportKeystorePath)) {

            Objects.requireNonNull(localConfiguration.getDatanodeTransportCertificatePassword(),
                    "transport_certificate_password has to be configured for the keystore " + transportKeystorePath
            );

            rootCerts.put("transport-chain-CA-root", rootCertificateFinder.findRootCert(transportKeystorePath,
                    localConfiguration.getDatanodeTransportCertificatePassword(),
                    CertutilCert.DATANODE_KEY_ALIAS));

            tlsConfigurationSupplier.addTransportTlsConfig(config,
                    CertutilCert.DATANODE_KEY_ALIAS,
                    localConfiguration.getDatanodeTransportCertificate(),
                    localConfiguration.getDatanodeTransportCertificatePassword());

            config.put("plugins.security.allow_default_init_securityindex", "true");
            //config.put("plugins.security.authcz.admin_dn", "CN=kirk,OU=client,O=client,L=test,C=de");

            config.put("plugins.security.audit.type", "internal_opensearch");
            config.put("plugins.security.enable_snapshot_restore_privilege", "true");
            config.put("plugins.security.check_snapshot_restore_write_privileges", "true");
            config.put("plugins.security.restapi.roles_enabled", "all_access,security_rest_api_access");
            config.put("plugins.security.system_indices.enabled", "true");
            config.put("plugins.security.system_indices.indices", ".plugins-ml-model,.plugins-ml-task,.opendistro-alerting-config,.opendistro-alerting-alert*,.opendistro-anomaly-results*,.opendistro-anomaly-detector*,.opendistro-anomaly-checkpoints,.opendistro-anomaly-detection-state,.opendistro-reports-*,.opensearch-notifications-*,.opensearch-notebooks,.opensearch-observability,.opendistro-asynchronous-search-response*,.replication-metadata-store");
            config.put("node.max_local_storage_nodes", "3");
        }

        if (Files.exists(httpKeystorePath)) {

            Objects.requireNonNull(localConfiguration.getDatanodeHttpCertificatePassword(),
                    "http_certificate_password has to be configured for the keystore " + httpKeystorePath
            );
            rootCerts.put("http-chain-CA-root", rootCertificateFinder.findRootCert(httpKeystorePath,
                    localConfiguration.getDatanodeHttpCertificatePassword(),
                    CertutilHttp.DATANODE_KEY_ALIAS));

            tlsConfigurationSupplier.addHttpTlsConfig(config,
                    CertutilHttp.DATANODE_KEY_ALIAS,
                    localConfiguration.getDatanodeHttpCertificate(),
                    localConfiguration.getDatanodeHttpCertificatePassword());

        }

        if (!rootCerts.isEmpty()) {
            final Path trustStorePath = opensearchConfigDir.resolve(TRUSTSTORE_FILENAME);
            truststoreCreator.createTruststore(rootCerts,
                    truststorePassword,
                    trustStorePath
            );
            System.setProperty("javax.net.ssl.trustStore", trustStorePath.toAbsolutePath().toString());
            System.setProperty("javax.net.ssl.trustStorePassword", truststorePassword);
            tlsConfigurationSupplier.addTrustStoreTlsConfig(config, truststorePassword);
        }
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

    private void configureInitialAdmin(Path internalUsersFile, String adminUsername, String adminPassword) throws IOException {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        final Map<String, Object> map = mapper.readValue(new FileInputStream(internalUsersFile.toFile()), TypeReferences.MAP_STRING_OBJECT);
        final Map<String, Object> adminUserConfig = (Map) map.get("admin");

        map.remove("admin");
        map.put(adminUsername, adminUserConfig);

        final BCryptPasswordAlgorithm passwordAlgorithm = new BCryptPasswordAlgorithm(12);
        final String hashWithPrefix = passwordAlgorithm.hash(adminPassword);

        // remove the prefix and suffix, we need just the hash itself
        final String hash = hashWithPrefix.substring("{bcrypt}".length(), hashWithPrefix.indexOf("{salt}"));
        adminUserConfig.put("hash", hash);

        final FileOutputStream fos = new FileOutputStream(internalUsersFile.toFile());
        mapper.writeValue(fos, map);
    }

    @Override
    public OpensearchConfiguration get() {
        return configuration;
    }

}
