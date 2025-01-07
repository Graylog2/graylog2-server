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
package org.graylog.datanode.opensearch.configuration.beans.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import jakarta.inject.Inject;
import org.apache.commons.lang3.RandomStringUtils;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.configuration.OpensearchConfigurationException;
import org.graylog.datanode.configuration.variants.OpensearchCertificates;
import org.graylog.datanode.configuration.variants.OpensearchCertificatesProvider;
import org.graylog.datanode.opensearch.configuration.OpensearchConfigurationParams;
import org.graylog.datanode.process.configuration.beans.DatanodeConfigurationBean;
import org.graylog.datanode.process.configuration.beans.DatanodeConfigurationPart;
import org.graylog.datanode.process.configuration.files.KeystoreConfigFile;
import org.graylog.datanode.process.configuration.files.OpensearchSecurityConfigurationFile;
import org.graylog.security.certutil.CertConstants;
import org.graylog.security.certutil.csr.KeystoreInformation;
import org.graylog2.security.JwtSecret;
import org.graylog2.security.TruststoreCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class OpensearchSecurityConfigurationBean implements DatanodeConfigurationBean<OpensearchConfigurationParams> {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchSecurityConfigurationBean.class);

    private static final String KEYSTORE_FORMAT = "PKCS12";
    private static final String TRUSTSTORE_FORMAT = "PKCS12";

    /**
     * This filename is used only internally - we copy user-provided certificates to this location and
     * we configure opensearch to read this file. It doesn't have to match naming provided by user.
     * The target configuration is regenerated during each startup, so it could also be a random filename
     * as long as we use the same name as a copy-target and opensearch config property.
     */
    private static final String TARGET_DATANODE_HTTP_KEYSTORE_FILENAME = "http-keystore.p12";
    /**
     * This filename is used only internally - we copy user-provided certificates to this location and
     * we configure opensearch to read this file. It doesn't have to match naming provided by user.
     * The target configuration is regenerated during each startup, so it could also be a random filename
     * as long as we use the same name as a copy-target and opensearch config property.
     */
    private static final String TARGET_DATANODE_TRANSPORT_KEYSTORE_FILENAME = "transport-keystore.p12";

    private static final Path TRUSTSTORE_FILE = Path.of("datanode-truststore.p12");

    private final Set<OpensearchCertificatesProvider> opensearchCertificatesProviders;
    private final Configuration localConfiguration;
    private final JwtSecret jwtSecret;

    @Inject
    public OpensearchSecurityConfigurationBean(Set<OpensearchCertificatesProvider> opensearchCertificatesProviders,
                                               final Configuration localConfiguration,
                                               final JwtSecret jwtSecret) {
        this.opensearchCertificatesProviders = opensearchCertificatesProviders;
        this.localConfiguration = localConfiguration;
        this.jwtSecret = jwtSecret;
    }

    @Override
    public DatanodeConfigurationPart buildConfigurationPart(OpensearchConfigurationParams opensearchConfigurationParams) {

        final DatanodeConfigurationPart.Builder configurationBuilder = DatanodeConfigurationPart.builder();

        Optional<OpensearchCertificates> opensearchCertificates = opensearchCertificatesProviders.stream()
                .filter(s -> s.isConfigured(localConfiguration))
                .findFirst()
                .map(OpensearchCertificatesProvider::build);

        configurationBuilder.securityConfigured(opensearchCertificates.isPresent()); // Caution, this may include insecure_startup config with no certs!

        final String truststorePassword = RandomStringUtils.randomAlphabetic(256);

        final TruststoreCreator truststoreCreator = TruststoreCreator.newDefaultJvm()
                .addCertificates(opensearchConfigurationParams.trustedCertificates());

        final Optional<KeystoreInformation> httpCert = opensearchCertificates
                .map(OpensearchCertificates::getHttpCertificate);

        final Optional<KeystoreInformation> transportCert = opensearchCertificates
                .map(OpensearchCertificates::getTransportCertificate);

        httpCert.ifPresent(cert -> {
            try {
                configurationBuilder.httpCertificate(cert);
                configurationBuilder.withConfigFile(new KeystoreConfigFile(Path.of(TARGET_DATANODE_HTTP_KEYSTORE_FILENAME), cert));
                truststoreCreator.addFromKeystore(cert, CertConstants.DATANODE_KEY_ALIAS);
                logCertificateInformation("HTTP certificate", cert);
            } catch (GeneralSecurityException | IOException e) {
                throw new OpensearchConfigurationException(e);
            }
        });

        transportCert.ifPresent(cert -> {
            try {
                configurationBuilder.transportCertificate(cert);
                configurationBuilder.withConfigFile(new KeystoreConfigFile(Path.of(TARGET_DATANODE_TRANSPORT_KEYSTORE_FILENAME), cert));
                truststoreCreator.addFromKeystore(cert, CertConstants.DATANODE_KEY_ALIAS);
                logCertificateInformation("Transport certificate", cert);
            } catch (GeneralSecurityException | IOException e) {
                throw new OpensearchConfigurationException(e);
            }
        });

        return configurationBuilder
                .properties(properties(httpCert, transportCert))
                .keystoreItems(keystoreItems(truststorePassword, httpCert, transportCert))
                .javaOpts(javaOptions(truststorePassword))
                .trustStore(truststoreCreator.getTruststore())
                .withConfigFile(new KeystoreConfigFile(TRUSTSTORE_FILE, truststoreCreator.toKeystoreInformation(truststorePassword.toCharArray())))
                .withConfigFile(new OpensearchSecurityConfigurationFile(jwtSecret))
                .build();
    }


    private Map<String, String> properties(Optional<KeystoreInformation> httpCert, Optional<KeystoreInformation> transportCert) {
        final ImmutableMap.Builder<String, String> config = ImmutableMap.builder();

        if (localConfiguration.getOpensearchAuditLog() != null && !localConfiguration.getOpensearchAuditLog().isBlank()) {
            config.put("plugins.security.audit.type", localConfiguration.getOpensearchAuditLog());
        }

        // enable admin access via the REST API
        config.put("plugins.security.restapi.admin.enabled", "true");


        if (httpCert.isPresent() && transportCert.isPresent()) {
            config.putAll(commonSecurityConfig());

            config.put("plugins.security.ssl.transport.keystore_type", KEYSTORE_FORMAT);
            config.put("plugins.security.ssl.transport.keystore_filepath", TARGET_DATANODE_TRANSPORT_KEYSTORE_FILENAME);
            config.put("plugins.security.ssl.transport.keystore_alias", CertConstants.DATANODE_KEY_ALIAS);

            config.put("plugins.security.ssl.transport.truststore_type", TRUSTSTORE_FORMAT);
            config.put("plugins.security.ssl.transport.truststore_filepath", TRUSTSTORE_FILE.toString());

            config.put("plugins.security.ssl.http.enabled", "true");

            config.put("plugins.security.ssl.http.keystore_type", KEYSTORE_FORMAT);
            config.put("plugins.security.ssl.http.keystore_filepath", TARGET_DATANODE_HTTP_KEYSTORE_FILENAME);
            config.put("plugins.security.ssl.http.keystore_alias", CertConstants.DATANODE_KEY_ALIAS);

            config.put("plugins.security.ssl.http.truststore_type", TRUSTSTORE_FORMAT);
            config.put("plugins.security.ssl.http.truststore_filepath", TRUSTSTORE_FILE.toString());

            // enable client cert auth
            config.put("plugins.security.ssl.http.clientauth_mode", "OPTIONAL");
        } else {
            config.put("plugins.security.disabled", "true");
            config.put("plugins.security.ssl.http.enabled", "false");
        }
        return config.build();
    }

    private List<String> javaOptions(String truststorePassword) {
        final ImmutableList.Builder<String> builder = ImmutableList.builder();
        builder.add("-Djavax.net.ssl.trustStore=" + TRUSTSTORE_FILE);
        builder.add("-Djavax.net.ssl.trustStorePassword=" + truststorePassword);
        builder.add("-Djavax.net.ssl.trustStoreType=pkcs12");
        return builder.build();
    }

    private Map<String, String> commonSecurityConfig() {
        final ImmutableMap.Builder<String, String> config = ImmutableMap.builder();
        config.put("plugins.security.disabled", "false");

        config.put("plugins.security.nodes_dn", "CN=*");
        config.put("plugins.security.allow_default_init_securityindex", "true");
        //config.put("plugins.security.authcz.admin_dn", "CN=kirk,OU=client,O=client,L=test,C=de");

        config.put("plugins.security.enable_snapshot_restore_privilege", "true");
        config.put("plugins.security.check_snapshot_restore_write_privileges", "true");
        config.put("plugins.security.restapi.roles_enabled", "all_access,security_rest_api_access,readall");
        config.put("plugins.security.system_indices.enabled", "true");
        config.put("plugins.security.system_indices.indices", ".plugins-ml-model,.plugins-ml-task,.opendistro-alerting-config,.opendistro-alerting-alert*,.opendistro-anomaly-results*,.opendistro-anomaly-detector*,.opendistro-anomaly-checkpoints,.opendistro-anomaly-detection-state,.opendistro-reports-*,.opensearch-notifications-*,.opensearch-notebooks,.opensearch-observability,.opendistro-asynchronous-search-response*,.replication-metadata-store");

        return config.build();
    }

    private Map<String, String> keystoreItems(String truststorePassword, Optional<KeystoreInformation> httpCert, Optional<KeystoreInformation> transportCert) {
        final ImmutableMap.Builder<String, String> config = ImmutableMap.builder();
        config.put("plugins.security.ssl.transport.truststore_password_secure", new String(truststorePassword));
        config.put("plugins.security.ssl.http.truststore_password_secure", new String(truststorePassword));
        httpCert.ifPresent(c -> config.put("plugins.security.ssl.http.keystore_password_secure", new String(c.password())));
        transportCert.ifPresent(c -> config.put("plugins.security.ssl.transport.keystore_password_secure", new String(c.password())));
        return config.build();
    }


    private void logCertificateInformation(String certificateType, KeystoreInformation keystore) {
        try {
            final KeyStore instance = keystore.loadKeystore();
            final Enumeration<String> aliases = instance.aliases();
            while (aliases.hasMoreElements()) {
                final Certificate cert = instance.getCertificate(aliases.nextElement());
                if (cert instanceof X509Certificate x509Certificate) {
                    final String alternativeNames = x509Certificate.getSubjectAlternativeNames()
                            .stream()
                            .map(san -> san.get(1))
                            .map(Object::toString)
                            .collect(Collectors.joining(", "));
                    LOG.info("Opensearch {} has following alternative names: {}", certificateType, alternativeNames);
                    LOG.info("Opensearch {} has following serial number: {}", certificateType, ((X509Certificate) cert).getSerialNumber());
                    LOG.info("Opensearch {} has following validity: {} - {}", certificateType, ((X509Certificate) cert).getNotBefore(), ((X509Certificate) cert).getNotAfter());
                }
            }
        } catch (Exception e) {
            throw new OpensearchConfigurationException("Failed to load kestore", e);
        }
    }
}
