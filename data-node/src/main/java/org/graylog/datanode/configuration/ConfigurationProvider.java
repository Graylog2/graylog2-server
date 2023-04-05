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
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.process.OpensearchConfiguration;
import org.graylog2.jackson.TypeReferences;
import org.graylog2.security.hashing.BCryptPasswordAlgorithm;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Singleton
public class ConfigurationProvider implements Provider<OpensearchConfiguration> {

    private final OpensearchConfiguration configuration;

    @Inject
    public ConfigurationProvider(Configuration localConfiguration, DataNodeConfig sharedConfiguration) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException, UnrecoverableKeyException {
        final var cfg = sharedConfiguration.test();

        final Path opensearchConfigDir = Path.of(localConfiguration.getOpensearchLocation()).resolve("config");

        final LinkedHashMap<String, String> config = new LinkedHashMap<>();
        config.put("path.data", Path.of(localConfiguration.getOpensearchDataLocation()).resolve(localConfiguration.getDatanodeNodeName()).toAbsolutePath().toString());
        config.put("path.logs", Path.of(localConfiguration.getOpensearchLogsLocation()).resolve(localConfiguration.getDatanodeNodeName()).toAbsolutePath().toString());
        if(localConfiguration.isSingleNodeOnly()) {
            config.put("discovery.type", "single-node");
        } else {
            config.put("cluster.initial_master_nodes", "node1");
        }

        // listen on all interfaces
        config.put("network.bind_host", "0.0.0.0");

        localConfiguration.getOpensearchNetworkHostHost().ifPresent(
                networkHost -> config.put("network.host", networkHost));


        final Path transportKeystorePath = Path.of(localConfiguration.getOpensearchConfigLocation())
                .resolve(localConfiguration.getDatanodeTransportCertificate());


        final Path httpKeystorePath = Path.of(localConfiguration.getOpensearchConfigLocation())
                .resolve(localConfiguration.getDatanodeHttpCertificate());

        if (Files.exists(transportKeystorePath) && Files.exists(httpKeystorePath)) {
            config.put("plugins.security.disabled", "false");
            config.put("plugins.security.ssl.http.enabled", "true");

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
            config.put("plugins.security.ssl.http.enabled", "false");
        }


        if (Files.exists(transportKeystorePath)) {

            Objects.requireNonNull(localConfiguration.getDatanodeTransportCertificatePassword(),
                    "transport_certificate_password has to be configured for the keystore " + transportKeystorePath
            );

            KeyStore nodeKeystore = loadKeystore(transportKeystorePath, localConfiguration.getDatanodeTransportCertificatePassword());
            extractCertificates(opensearchConfigDir, "transport", nodeKeystore, localConfiguration.getDatanodeTransportCertificatePassword());

            config.put("plugins.security.ssl.transport.pemcert_filepath", "transport.pem");
            config.put("plugins.security.ssl.transport.pemkey_filepath", "transport-key.pem");
            config.put("plugins.security.ssl.transport.pemtrustedcas_filepath", "transport-ca.pem");

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

            KeyStore httpKeystore = loadKeystore(httpKeystorePath, localConfiguration.getDatanodeHttpCertificatePassword());

            final String truststorePassword = UUID.randomUUID().toString();
            final Path trustStorePath = createTruststore(httpKeystore, truststorePassword, opensearchConfigDir);
            System.setProperty("javax.net.ssl.trustStore", trustStorePath.toAbsolutePath().toString());
            System.setProperty("javax.net.ssl.trustStorePassword", truststorePassword);

            extractCertificates(opensearchConfigDir, "http", httpKeystore, localConfiguration.getDatanodeHttpCertificatePassword());

            config.put("plugins.security.ssl.http.pemcert_filepath", "http.pem");
            config.put("plugins.security.ssl.http.pemkey_filepath", "http-key.pem");
            config.put("plugins.security.ssl.http.pemtrustedcas_filepath", "http-ca.pem");
        }

        configuration = new OpensearchConfiguration(
                localConfiguration.getOpensearchVersion(),
                Path.of(localConfiguration.getOpensearchLocation()),
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

    private Path createTruststore(KeyStore keystorePath, String password, Path opensearchConfigDir) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        trustStore.load(null, null);


        final Certificate[] certs = keystorePath.getCertificateChain("datanode");

        for (Certificate cert : certs) {
            if (cert instanceof final X509Certificate x509Certificate) {
                final String alias = x509Certificate.getSubjectX500Principal().getName();
                trustStore.setCertificateEntry(alias, x509Certificate);
            }
        }

        final Path truststorePath = opensearchConfigDir.resolve("datanode-truststore.jks");
        trustStore.store(new FileOutputStream(truststorePath.toFile()), password.toCharArray());
        return truststorePath;
    }

    private void extractCertificates(Path opensearchConfigDir, String prefix, KeyStore nodeKeystore, String password) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {
        final Key datanodePrivateKey = nodeKeystore.getKey("datanode", password.toCharArray());
        writePem(opensearchConfigDir.resolve(prefix + "-key.pem"), datanodePrivateKey);

        final Certificate datanodeCert = nodeKeystore.getCertificate("datanode");
        writePem(opensearchConfigDir.resolve(prefix + ".pem"), datanodeCert);

        final Certificate[] certChain = nodeKeystore.getCertificateChain("datanode");
        final Certificate caCertificate = Arrays.stream(certChain)
                .filter(c -> ((X509Certificate) c).getSubjectX500Principal().getName().equals("CN=ca"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not extract CA certificate"));

        writePem(opensearchConfigDir.resolve(prefix + "-ca.pem"), caCertificate);
    }

    private static KeyStore loadKeystore(Path keystorePath, String password) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        KeyStore nodeKeystore = KeyStore.getInstance("PKCS12");
        final FileInputStream is = new FileInputStream(keystorePath.toFile());
        nodeKeystore.load(is, password.toCharArray());
        return nodeKeystore;
    }


    private static void writePem(Path path, Object object) throws IOException {
        FileWriter writer = new FileWriter(path.toFile(), StandardCharsets.UTF_8);
        JcaPEMWriter pemWriter = new JcaPEMWriter(writer);
        pemWriter.writeObject(object);
        pemWriter.flush();
        pemWriter.close();
    }

    @Override
    public OpensearchConfiguration get() {
        return configuration;
    }

}
