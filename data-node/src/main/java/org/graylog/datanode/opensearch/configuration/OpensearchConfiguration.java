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
import org.graylog.datanode.opensearch.configuration.beans.OpensearchConfigurationPart;
import org.graylog.datanode.opensearch.configuration.beans.files.ConfigFile;
import org.graylog.datanode.process.Environment;
import org.graylog.security.certutil.csr.KeystoreInformation;
import org.graylog.shaded.opensearch2.org.apache.http.HttpHost;

import java.security.KeyStore;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public record OpensearchConfiguration(
        OpensearchDistribution opensearchDistribution,
        DatanodeDirectories datanodeDirectories,
        String hostname,
        int httpPort,
        Set<OpensearchConfigurationPart> configurationParts
) {
    public Map<String, Object> asMap() {
        Map<String, Object> config = new LinkedHashMap<>();

        config.put("node.roles", buildRolesList()); // this needs special treatment as it's as an aggregation of other configuration parts

        configurationParts.stream()
                .map(OpensearchConfigurationPart::properties)
                .forEach(config::putAll);
        return config;
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

        configurationParts.stream().map(OpensearchConfigurationPart::javaOpts)
                .forEach(javaOpts::addAll);

        env.put("OPENSEARCH_JAVA_OPTS", String.join(" ", javaOpts));
        env.put("OPENSEARCH_PATH_CONF", datanodeDirectories.getOpensearchProcessConfigurationDir().toString());
        return env;
    }

    public HttpHost getRestBaseUrl() {
        return new HttpHost(hostname(), httpPort(), isHttpsEnabled() ? "https" : "http");
    }

    public boolean isHttpsEnabled() {
        return httpCertificate().isPresent();
    }

    /**
     * Are there any {@link  org.graylog.datanode.configuration.variants.OpensearchCertificatesProvider} configured?
     */
    public boolean securityConfigured() {
        return configurationParts.stream().anyMatch(OpensearchConfigurationPart::securityConfigured);
    }


    public Map<String, String> getKeystoreItems() {
        final ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        configurationParts.stream()
                .map(OpensearchConfigurationPart::keystoreItems)
                .forEach(builder::putAll);

        return builder.build();
    }

    public KeyStore trustStore() {
        return configurationParts.stream()
                .map(OpensearchConfigurationPart::trustStore)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("This should not happen, truststore should always be present"));
    }

    public Optional<KeystoreInformation> httpCertificate() {
        return configurationParts.stream()
                .map(OpensearchConfigurationPart::httpCertificate)
                .filter(Objects::nonNull)
                .findFirst();
    }

    public Optional<KeystoreInformation> transportCertificate() {
        return configurationParts.stream()
                .map(OpensearchConfigurationPart::transportCertificate)
                .filter(Objects::nonNull)
                .findFirst();
    }

    public List<ConfigFile> configFiles() {
        return configurationParts.stream()
                .flatMap(cp -> cp.configFiles().stream())
                .collect(Collectors.toList());
    }
}
