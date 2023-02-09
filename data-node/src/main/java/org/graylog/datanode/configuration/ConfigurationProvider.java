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

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.graylog.datanode.process.OpensearchConfiguration;

import javax.inject.Inject;
import javax.inject.Named;
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
import java.util.List;

@Singleton
public class ConfigurationProvider implements Provider<OpensearchConfiguration> {

    private final OpensearchConfiguration configuration;

    @Inject
    public ConfigurationProvider(@Named("opensearch_version") String opensearchVersion,
                                 @Named("opensearch_location") String opensearchLocation,
                                 @Named("opensearch_data_location") String opensearchDataLocation,
                                 @Named("opensearch_logs_location") String opensearchLogsLocation,
                                 @Named("opensearch_config_location") String opensearchConfigLocation,
                                 @Named("datanode_node_name") String nodeName,
                                 @Named("opensearch_http_port") int opensearchHttpPort,
                                 @Named("opensearch_transport_port") int opensearchTransportPort,
                                 @Named("opensearch_discovery_seed_hosts") List<String> discoverySeedHosts


    ) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException, UnrecoverableKeyException {




        final LinkedHashMap<String, String> config = new LinkedHashMap<>();
        config.put("path.data", Path.of(opensearchDataLocation).resolve(nodeName).toAbsolutePath().toString());
        config.put("path.logs", Path.of(opensearchLogsLocation).resolve(nodeName).toAbsolutePath().toString());
        //config.put("discovery.type", "single-node");

        config.put("cluster.initial_master_nodes", "node1");

        final Path transportKeystorePath = Path.of(opensearchConfigLocation).resolve("datanode-transport-certificates.p12");
        final Path httpKeystorePath = Path.of(opensearchConfigLocation).resolve("datanode-http-certificates.p12");

        if(Files.exists(transportKeystorePath) && Files.exists(httpKeystorePath)) {
            config.put("plugins.security.disabled", "false");
            config.put("plugins.security.ssl.http.enabled", "true");
        } else {
            config.put("plugins.security.disabled", "true");
            config.put("plugins.security.ssl.http.enabled", "false");
        }

        final Path opensearchConfigDir = Path.of(opensearchLocation).resolve("config");

        if(Files.exists(transportKeystorePath)) {

            KeyStore nodeKeystore = loadKeystore(transportKeystorePath, "password");
            extractCertificates(opensearchConfigDir, "transport", nodeKeystore, "password");

            config.put("plugins.security.ssl.transport.pemcert_filepath", "transport.pem");
            config.put("plugins.security.ssl.transport.pemkey_filepath", "transport-key.pem");
            config.put("plugins.security.ssl.transport.pemtrustedcas_filepath", "transport-ca.pem");

            config.put("plugins.security.allow_unsafe_democertificates", "true");
            config.put("plugins.security.allow_default_init_securityindex", "true");
            config.put("plugins.security.authcz.admin_dn", "CN=kirk,OU=client,O=client,L=test,C=de");

            config.put("plugins.security.audit.type", "internal_opensearch");
            config.put("plugins.security.enable_snapshot_restore_privilege","true");
            config.put("plugins.security.check_snapshot_restore_write_privileges","true");
            config.put("plugins.security.restapi.roles_enabled", "all_access,security_rest_api_access");
            config.put("plugins.security.system_indices.enabled", "true");
            config.put("plugins.security.system_indices.indices", ".plugins-ml-model,.plugins-ml-task,.opendistro-alerting-config,.opendistro-alerting-alert*,.opendistro-anomaly-results*,.opendistro-anomaly-detector*,.opendistro-anomaly-checkpoints,.opendistro-anomaly-detection-state,.opendistro-reports-*,.opensearch-notifications-*,.opensearch-notebooks,.opensearch-observability,.opendistro-asynchronous-search-response*,.replication-metadata-store");
            config.put("node.max_local_storage_nodes", "3");
        }

        if(Files.exists(httpKeystorePath)) {

            KeyStore httpKeystore = loadKeystore(httpKeystorePath, "password");
            final Path trustStorePath = createTruststore(httpKeystore, "password", opensearchConfigDir);
            System.setProperty("javax.net.ssl.trustStore", trustStorePath.toAbsolutePath().toString());
            System.setProperty("javax.net.ssl.trustStorePassword", "password");

            extractCertificates(opensearchConfigDir, "http", httpKeystore, "password");

            config.put("plugins.security.ssl.http.pemcert_filepath", "http.pem");
            config.put("plugins.security.ssl.http.pemkey_filepath", "http-key.pem");
            config.put("plugins.security.ssl.http.pemtrustedcas_filepath", "http-ca.pem");
        }



        configuration = new OpensearchConfiguration(
                opensearchVersion,
                Path.of(opensearchLocation),
                opensearchHttpPort,
                opensearchTransportPort,
                "datanode-cluster",
                nodeName,
                Collections.emptyList(),
                Collections.emptyList(),
                discoverySeedHosts,
                config
        );
    }

    private Path createTruststore(KeyStore keystorePath, String password, Path opensearchConfigDir) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        trustStore.load(null, null);


        final Certificate[] certs = keystorePath.getCertificateChain("datanode");

        for(Certificate cert : certs) {
            if(cert instanceof final X509Certificate x509Certificate) {
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
