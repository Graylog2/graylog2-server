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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class OpensearchSecurityConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchSecurityConfiguration.class);

    private static final String KEYSTORE_FORMAT = "PKCS12";
    private static final String TRUSTSTORE_FORMAT = "PKCS12";
    private static final String TRUSTSTORE_FILENAME = "datanode-truststore.p12";

    private final KeystoreInformation transportCertificate;
    private final KeystoreInformation httpCertificate;
    private KeystoreInformation truststore;

    public OpensearchSecurityConfiguration(KeystoreInformation transportCertificate, KeystoreInformation httpCertificate) {
        this.transportCertificate = transportCertificate;
        this.httpCertificate = httpCertificate;
    }

    public static OpensearchSecurityConfiguration disabled() {
        return new OpensearchSecurityConfiguration(null, null);
    }

    /**
     * Caution: side effects!
     *
     * This method will take the current security setup and apply it to the managed opensearch. It will change the
     * initial set of opensearch users, it will create and persist a truststore that will be set as a system-wide
     * truststore.
     */
    public OpensearchSecurityConfiguration configure(Configuration localConfiguration) throws GeneralSecurityException, IOException {
        if (securityEnabled()) {

            logCertificateInformation("transport certificate", transportCertificate);
            logCertificateInformation("HTTP certificate", httpCertificate);

            final Path opensearchConfigDir = Path.of(localConfiguration.getOpensearchConfigLocation()).resolve("opensearch");

            final Path trustStorePath = opensearchConfigDir.resolve(TRUSTSTORE_FILENAME);
            final String truststorePassword = RandomStringUtils.randomAlphabetic(256);

            this.truststore = TruststoreCreator.newTruststore()
                    .addRootCert("transport-chain-CA-root", transportCertificate, CertConstants.DATANODE_KEY_ALIAS)
                    .addRootCert("http-chain-CA-root", httpCertificate, CertConstants.DATANODE_KEY_ALIAS)
                    .persist(trustStorePath, truststorePassword.toCharArray());

            System.setProperty("javax.net.ssl.trustStore", trustStorePath.toAbsolutePath().toString());
            System.setProperty("javax.net.ssl.trustStorePassword", truststorePassword);

            configureInitialAdmin(localConfiguration, opensearchConfigDir, localConfiguration.getRestApiUsername(), localConfiguration.getRestApiPassword());
        }
        return this;
    }

    public Map<String, String> getProperties() throws GeneralSecurityException, IOException {
        final ImmutableMap.Builder<String, String> config = ImmutableMap.builder();
        if (securityEnabled()) {
            config.putAll(commonSecureConfig());

            config.put("plugins.security.ssl.transport.keystore_type", KEYSTORE_FORMAT);
            config.put("plugins.security.ssl.transport.keystore_filepath", transportCertificate.location().getFileName().toString()); // todo: this should be computed as a relative path
            config.put("plugins.security.ssl.transport.keystore_password", transportCertificate.passwordAsString());
            config.put("plugins.security.ssl.transport.keystore_alias", CertConstants.DATANODE_KEY_ALIAS);

            config.put("plugins.security.ssl.transport.truststore_type", TRUSTSTORE_FORMAT);
            config.put("plugins.security.ssl.transport.truststore_filepath", TRUSTSTORE_FILENAME);
            config.put("plugins.security.ssl.transport.truststore_password", truststore.passwordAsString());

            // this disables hostname verification for transport. It's a workaround for localnode communication
            // via SSL, where Opensearch still tries to communicate with 'localhost' and not the publish_host or other
            // configured node names.
            config.put("plugins.security.ssl.transport.enforce_hostname_verification", "false");


            config.put("plugins.security.ssl.http.enabled", "true");

            config.put("plugins.security.ssl.http.keystore_type", KEYSTORE_FORMAT);
            config.put("plugins.security.ssl.http.keystore_filepath",  httpCertificate.location().getFileName().toString());  // todo: this should be computed as a relative path
            config.put("plugins.security.ssl.http.keystore_password", httpCertificate.passwordAsString());
            config.put("plugins.security.ssl.http.keystore_alias", CertConstants.DATANODE_KEY_ALIAS);

            config.put("plugins.security.ssl.http.truststore_type", TRUSTSTORE_FORMAT);
            config.put("plugins.security.ssl.http.truststore_filepath", TRUSTSTORE_FILENAME);
            config.put("plugins.security.ssl.http.truststore_password", truststore.passwordAsString());
        } else {
            config.put("plugins.security.disabled", "true");
            config.put("plugins.security.ssl.http.enabled", "false");
        }
        return config.build();
    }

    public boolean securityEnabled() {
        return !Objects.isNull(httpCertificate) && !Objects.isNull(transportCertificate);
    }

    public KeystoreInformation getTransportCertificate() {
        return transportCertificate;
    }

    public KeystoreInformation getHttpCertificate() {
        return httpCertificate;
    }

    public KeystoreInformation getTruststore() {
        return truststore;
    }

    protected ImmutableMap<String, String> commonSecureConfig() {
        final ImmutableMap.Builder<String, String> config = ImmutableMap.builder();

        config.put("plugins.security.disabled", "false");
        //config.put(SSL_PREFIX + "http.enabled", "true");

        config.put("plugins.security.nodes_dn", "CN=*");
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

    private void logCertificateInformation(String certificateType, KeystoreInformation keystore) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        final KeyStore instance = KeyStore.getInstance(KEYSTORE_FORMAT);
        try (final FileInputStream is = new FileInputStream(keystore.location().toFile())) {
            instance.load(is, keystore.password());
            final Enumeration<String> aliases = instance.aliases();
            while(aliases.hasMoreElements()) {
                final Certificate cert = instance.getCertificate(aliases.nextElement());
                if(cert instanceof X509Certificate x509Certificate) {
                    final String alternativeNames = x509Certificate.getSubjectAlternativeNames()
                            .stream()
                            .map(san -> san.get(1))
                            .map(Object::toString)
                            .collect(Collectors.joining(", "));
                    LOG.info("Opensearch {} has following alternative names: {}", certificateType, alternativeNames);
                }
            }
        }
    }
}
