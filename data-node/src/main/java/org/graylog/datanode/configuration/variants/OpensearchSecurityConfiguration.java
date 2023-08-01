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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.RandomStringUtils;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.configuration.TruststoreCreator;
import org.graylog.security.certutil.CertConstants;
import org.graylog2.jackson.TypeReferences;
import org.graylog2.security.hashing.BCryptPasswordAlgorithm;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.Objects;

public class OpensearchSecurityConfiguration {

    private static final String KEYSTORE_FORMAT = "PKCS12";
    private static final String TRUSTSTORE_FORMAT = "PKCS12";
    private static final String TRUSTSTORE_FILENAME = "datanode-truststore.p12";

    private final KeystoreInformation transportCertificate;
    private final KeystoreInformation httpCertificate;
    private final String truststorePassword;

    public OpensearchSecurityConfiguration(KeystoreInformation transportCertificate, KeystoreInformation httpCertificate) {
        this.transportCertificate = transportCertificate;
        this.httpCertificate = httpCertificate;
        this.truststorePassword = RandomStringUtils.randomAlphabetic(256);
    }

    public static OpensearchSecurityConfiguration disabled() {
        return new OpensearchSecurityConfiguration(null, null);
    }

    public Map<String, String> toOpensearchConfig(Configuration localConfiguration, Path opensearchConfigDir) throws GeneralSecurityException, IOException {

        final ImmutableMap.Builder<String, String> config = ImmutableMap.builder();
        config.putAll(commonConfig(localConfiguration));

        if (securityEnabled()) {

            config.putAll(commonSecureConfig());


            config.put("plugins.security.ssl.transport.keystore_type", KEYSTORE_FORMAT);
            config.put("plugins.security.ssl.transport.keystore_filepath", transportCertificate.location().getFileName().toString()); // todo: this should be computed as a relative path
            config.put("plugins.security.ssl.transport.keystore_password", transportCertificate.passwordAsString());
            config.put("plugins.security.ssl.transport.keystore_alias", CertConstants.DATANODE_KEY_ALIAS);

            config.put("plugins.security.ssl.transport.truststore_type", TRUSTSTORE_FORMAT);
            config.put("plugins.security.ssl.transport.truststore_filepath", TRUSTSTORE_FILENAME);
            config.put("plugins.security.ssl.transport.truststore_password", truststorePassword);


            config.put("plugins.security.ssl.http.enabled", "true");

            config.put("plugins.security.ssl.http.keystore_type", KEYSTORE_FORMAT);
            config.put("plugins.security.ssl.http.keystore_filepath",  httpCertificate.location().getFileName().toString());  // todo: this should be computed as a relative path
            config.put("plugins.security.ssl.http.keystore_password", httpCertificate.passwordAsString());
            config.put("plugins.security.ssl.http.keystore_alias", CertConstants.DATANODE_KEY_ALIAS);

            config.put("plugins.security.ssl.http.truststore_type", TRUSTSTORE_FORMAT);
            config.put("plugins.security.ssl.http.truststore_filepath", TRUSTSTORE_FILENAME);
            config.put("plugins.security.ssl.http.truststore_password", truststorePassword);


            // TODO: caution, side-effect
            final Path trustStorePath = opensearchConfigDir.resolve(TRUSTSTORE_FILENAME);
            TruststoreCreator.newTruststore()
                    .addRootCert("transport-chain-CA-root", transportCertificate, CertConstants.DATANODE_KEY_ALIAS)
                    .addRootCert("http-chain-CA-root", httpCertificate, CertConstants.DATANODE_KEY_ALIAS)
                    .persist(trustStorePath, truststorePassword.toCharArray());

            // TODO: caution, side-effect
            System.setProperty("javax.net.ssl.trustStore", trustStorePath.toAbsolutePath().toString());
            System.setProperty("javax.net.ssl.trustStorePassword", truststorePassword);

            // TODO: caution, side-effect
            configureInitialAdmin(localConfiguration, opensearchConfigDir, localConfiguration.getRestApiUsername(), localConfiguration.getRestApiPassword());

        } else {
            config.put("plugins.security.disabled", "true");
            config.put("plugins.security.ssl.http.enabled", "false");
        }
        return config.build();
    }

    /**
     * This is not part of the security setup, it should be moved elsewhere
     */
    @Deprecated(forRemoval = true)
    private ImmutableMap<String, String> commonConfig(final Configuration localConfiguration) {
        final ImmutableMap.Builder<String, String> config = ImmutableMap.builder();
        Objects.requireNonNull(localConfiguration.getConfigLocation(), "config_location setting is required!");
        localConfiguration.getOpensearchNetworkHostHost().ifPresent(
                networkHost -> config.put("network.host", networkHost));
        config.put("path.data", Path.of(localConfiguration.getOpensearchDataLocation()).resolve(localConfiguration.getDatanodeNodeName()).toAbsolutePath().toString());
        config.put("path.logs", Path.of(localConfiguration.getOpensearchLogsLocation()).resolve(localConfiguration.getDatanodeNodeName()).toAbsolutePath().toString());
        if (localConfiguration.isSingleNodeOnly()) {
            config.put("discovery.type", "single-node");
        } else {
            config.put("cluster.initial_master_nodes", "node1");
        }

        // listen on all interfaces
        config.put("network.bind_host", "0.0.0.0");

        return config.build();
    }

    public boolean securityEnabled() {
        return !Objects.isNull(httpCertificate) && !Objects.isNull(transportCertificate);
    }

    protected ImmutableMap<String, String> commonSecureConfig() {
        final ImmutableMap.Builder<String, String> config = ImmutableMap.builder();

        config.put("plugins.security.disabled", "false");
        //config.put(SSL_PREFIX + "http.enabled", "true");
        config.put("plugins.security.allow_default_init_securityindex", "true");
        //config.put("plugins.security.authcz.admin_dn", "CN=kirk,OU=client,O=client,L=test,C=de");

        config.put("plugins.security.audit.type", "internal_opensearch");
        config.put("plugins.security.enable_snapshot_restore_privilege", "true");
        config.put("plugins.security.check_snapshot_restore_write_privileges", "true");
        config.put("plugins.security.restapi.roles_enabled", "all_access,security_rest_api_access");
        config.put("plugins.security.system_indices.enabled", "true");
        config.put("plugins.security.system_indices.indices", ".plugins-ml-model,.plugins-ml-task,.opendistro-alerting-config,.opendistro-alerting-alert*,.opendistro-anomaly-results*,.opendistro-anomaly-detector*,.opendistro-anomaly-checkpoints,.opendistro-anomaly-detection-state,.opendistro-reports-*,.opensearch-notifications-*,.opensearch-notebooks,.opensearch-observability,.opendistro-asynchronous-search-response*,.replication-metadata-store");
        config.put("node.max_local_storage_nodes", "3");

        return config.build();
    }

    protected void configureInitialAdmin(final Configuration localConfiguration, final Path opensearchConfigDir,
                                         final String adminUsername,
                                         final String adminPassword) throws IOException {
        final Path internalUsersFile = opensearchConfigDir.resolve("opensearch-security").resolve("internal_users.yml");

        Objects.requireNonNull(localConfiguration.getRestApiUsername(),
                "rest_api_username has to be configured the usage of secured Opensearch REST api"
        );

        Objects.requireNonNull(localConfiguration.getRestApiPassword(),
                "rest_api_password has to be configured the usage of secured Opensearch REST api"
        );

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
}
