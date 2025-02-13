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
package org.graylog.datanode.opensearch.configuration;

import com.google.common.collect.ImmutableMap;
import jakarta.annotation.Nonnull;
import org.graylog.datanode.OpensearchDistribution;
import org.graylog.datanode.configuration.DatanodeDirectories;
import org.graylog.datanode.configuration.OpensearchConfigurationDir;
import org.graylog.datanode.process.configuration.beans.DatanodeConfigurationPart;
import org.graylog.datanode.process.configuration.files.DatanodeConfigFile;
import org.graylog.datanode.process.configuration.files.YamlConfigFile;
import org.graylog.datanode.process.Environment;
import org.graylog.security.certutil.csr.KeystoreInformation;
import org.graylog.shaded.opensearch2.org.apache.http.HttpHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.security.KeyStore;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class OpensearchConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchConfiguration.class);

    private final OpensearchDistribution opensearchDistribution;
    private final String hostname;
    private final int httpPort;
    private final List<DatanodeConfigurationPart> configurationParts;
    private final OpensearchConfigurationDir opensearchConfigurationDir;
    private final DatanodeDirectories datanodeDirectories;

    public OpensearchConfiguration(OpensearchDistribution opensearchDistribution, DatanodeDirectories datanodeDirectories, String hostname, int httpPort, List<DatanodeConfigurationPart> configurationParts) {
        this.opensearchDistribution = opensearchDistribution;
        this.hostname = hostname;
        this.httpPort = httpPort;
        this.configurationParts = configurationParts;
        this.datanodeDirectories = datanodeDirectories;
        this.opensearchConfigurationDir = datanodeDirectories.createUniqueOpensearchProcessConfigurationDir();
    }

    @Nonnull
    private String buildRolesList() {
        return configurationParts.stream()
                .flatMap(cfg -> cfg.nodeRoles().stream())
                .collect(Collectors.joining(","));
    }

    public Environment getEnv() {
        final Environment env = new Environment(System.getenv());

        List<String> javaOpts = new LinkedList<>();

        configurationParts.stream().map(DatanodeConfigurationPart::javaOpts)
                .forEach(javaOpts::addAll);

        env.put("OPENSEARCH_JAVA_OPTS", String.join(" ", javaOpts));
        env.put("OPENSEARCH_PATH_CONF", opensearchConfigurationDir.configurationRoot().toString());
        return env;
    }

    public HttpHost getRestBaseUrl() {
        return new HttpHost(hostname, httpPort, isHttpsEnabled() ? "https" : "http");
    }

    public boolean isHttpsEnabled() {
        return httpCertificate().isPresent();
    }

    /**
     * Are there any {@link  org.graylog.datanode.configuration.variants.OpensearchCertificatesProvider} configured?
     */
    public boolean securityConfigured() {
        return configurationParts.stream().anyMatch(DatanodeConfigurationPart::securityConfigured);
    }


    public Map<String, String> getKeystoreItems() {
        final ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        configurationParts.stream()
                .map(DatanodeConfigurationPart::keystoreItems)
                .forEach(builder::putAll);

        return builder.build();
    }

    public KeyStore trustStore() {
        return configurationParts.stream()
                .map(DatanodeConfigurationPart::trustStore)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("This should not happen, truststore should always be present"));
    }

    public Optional<KeystoreInformation> httpCertificate() {
        return configurationParts.stream()
                .map(DatanodeConfigurationPart::httpCertificate)
                .filter(Objects::nonNull)
                .findFirst();
    }

    public Optional<KeystoreInformation> transportCertificate() {
        return configurationParts.stream()
                .map(DatanodeConfigurationPart::transportCertificate)
                .filter(Objects::nonNull)
                .findFirst();
    }

    public List<DatanodeConfigFile> configFiles() {

        final List<DatanodeConfigFile> configFiles = new LinkedList<>();

        configurationParts.stream()
                .flatMap(cp -> cp.configFiles().stream())
                .forEach(configFiles::add);

        configFiles.add(new YamlConfigFile(Path.of("opensearch.yml"), opensearchYmlConfig()));

        return configFiles;
    }

    private Map<String, Object> opensearchYmlConfig() {
        Map<String, Object> config = new LinkedHashMap<>();

        // this needs special treatment as it's as an aggregation of other configuration parts
        config.put("node.roles", buildRolesList());

        configurationParts.stream()
                .map(DatanodeConfigurationPart::properties)
                .forEach(config::putAll);

        // now copy all the environment values to the configuration arguments. Opensearch won't do it for us,
        // because we are using tar distriburion and opensearch does this only for docker dist. See opensearch-env script
        // additionally, the env variables have to be prefixed with opensearch. (e.g. "opensearch.cluster.routing.allocation.disk.threshold_enabled")
        getEnv().getEnv().entrySet().stream()
                .filter(entry -> entry.getKey().matches("^opensearch\\.[a-z0-9_]+(?:\\.[a-z0-9_]+)+"))
                .peek(entry -> LOG.info("Detected pass-through opensearch property {}:{}", entry.getKey().substring("opensearch.".length()), entry.getValue()))
                .forEach(entry -> config.put(entry.getKey().substring("opensearch.".length()), entry.getValue()));
        return config;
    }

    public OpensearchDistribution getOpensearchDistribution() {
        return opensearchDistribution;
    }

    public OpensearchConfigurationDir getOpensearchConfigurationDir() {
        return opensearchConfigurationDir;
    }

    public DatanodeDirectories getDatanodeDirectories() {
        return datanodeDirectories;
    }
}
