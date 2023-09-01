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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.RandomStringUtils;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.configuration.TruststoreCreator;
import org.graylog.security.certutil.CertConstants;
import org.graylog2.jackson.TypeReferences;
import org.graylog2.security.hashing.BCryptPasswordAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;
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
    private final ObjectMapper objectMapper = new YAMLMapper();

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
    public OpensearchSecurityConfiguration configure(Configuration localConfiguration, byte[] signingKey) throws GeneralSecurityException, IOException {
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

            enableJwtInConfig(opensearchConfigDir, signingKey);
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

            // these were enabled to debug JWT
            config.put("plugins.security.audit.type", "debug");
            config.put("logger.org.opensearch.security", "debug");
            // config.put("logger.com.amazon.dlic.auth.http.jwt", "debug");

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

    private Map<String, Object> getMap(final Map<String, Object> map, final String... keys) {
        Map<String, Object> result = map;
        for(final String key: List.of(keys)) {
            result = (Map<String, Object>)result.get(key);
        };
        return result;
    }

    private void enableJwtInConfig(final Path opensearchConfigDir, final byte[] signingKey) throws IOException {
        final File file = opensearchConfigDir.resolve(Path.of("opensearch-security", "config.yml")).toFile();
        Map<String, Object> contents = objectMapper.readValue(file, new TypeReference<>() {});

        Map<String, Object> jwt_auth_domain = getMap(contents, "config", "dynamic", "authc", "jwt_auth_domain");
        jwt_auth_domain.put("http_enabled", true);
        jwt_auth_domain.put("transport_enabled", true);

        Map<String, Object> config = getMap(jwt_auth_domain, "http_authenticator", "config");
        config.put("signing_key", Base64.getEncoder().encodeToString(signingKey));
        config.put("roles_key", "os_roles");

        objectMapper.writeValue(file, contents);
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

        //config.put("plugins.security.audit.type", "internal_opensearch");
        config.put("plugins.security.enable_snapshot_restore_privilege", "true");
        config.put("plugins.security.check_snapshot_restore_write_privileges", "true");
        config.put("plugins.security.restapi.roles_enabled", "all_access,security_rest_api_access");
        config.put("plugins.security.system_indices.enabled", "true");
        config.put("plugins.security.system_indices.indices", ".plugins-ml-model,.plugins-ml-task,.opendistro-alerting-config,.opendistro-alerting-alert*,.opendistro-anomaly-results*,.opendistro-anomaly-detector*,.opendistro-anomaly-checkpoints,.opendistro-anomaly-detection-state,.opendistro-reports-*,.opensearch-notifications-*,.opensearch-notebooks,.opensearch-observability,.opendistro-asynchronous-search-response*,.replication-metadata-store");
        config.put("node.max_local_storage_nodes", "3");

        return config.build();
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
