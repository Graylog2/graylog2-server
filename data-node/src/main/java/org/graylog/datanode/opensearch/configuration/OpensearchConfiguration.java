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

import com.google.common.collect.ImmutableList;
import jakarta.annotation.Nonnull;
import org.graylog.datanode.OpensearchDistribution;
import org.graylog.datanode.configuration.DatanodeDirectories;
import org.graylog.datanode.configuration.OpensearchConfigurationDir;
import org.graylog.datanode.process.Environment;
import org.graylog.datanode.process.configuration.beans.DatanodeConfigurationPart;
import org.graylog.datanode.process.configuration.beans.OpensearchKeystoreItem;
import org.graylog.datanode.process.configuration.files.DatanodeConfigFile;
import org.graylog.datanode.process.configuration.files.YamlConfigFile;
import org.graylog.security.certutil.csr.KeystoreInformation;
import org.graylog.shaded.opensearch2.org.apache.http.HttpHost;

import java.nio.file.Path;
import java.security.KeyStore;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class OpensearchConfiguration {

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
        return new Environment(System.getenv())
                .withOpensearchJavaHome(opensearchDistribution.getOpensearchJavaHome())
                .withOpensearchJavaOpts(getJavaOpts())
                .withOpensearchPathConf(opensearchConfigurationDir.configurationRoot());
    }

    @Nonnull
    private List<String> getJavaOpts() {
        return configurationParts.stream()
                .flatMap(part -> part.javaOpts().stream())
                .collect(Collectors.toList());
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


    public Collection<OpensearchKeystoreItem> getKeystoreItems() {
        final ImmutableList.Builder<OpensearchKeystoreItem> builder = ImmutableList.builder();
        configurationParts.stream()
                .map(DatanodeConfigurationPart::keystoreItems)
                .forEach(builder::addAll);
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

    public List<String> opensearchRoles() {
        return configurationParts.stream()
                .flatMap(cfg -> cfg.nodeRoles().stream())
                .collect(Collectors.toList());
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

    public List<String> warnings() {
        return configurationParts.stream()
                .flatMap(part -> part.warnings().stream())
                .collect(Collectors.toList());
    }
}
