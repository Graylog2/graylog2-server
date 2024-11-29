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
import com.google.common.collect.ImmutableMap;
import jakarta.annotation.Nonnull;
import org.apache.commons.exec.OS;
import org.graylog.datanode.OpensearchDistribution;
import org.graylog.datanode.configuration.DatanodeDirectories;
import org.graylog.datanode.opensearch.configuration.beans.OpensearchConfigurationDirModifier;
import org.graylog.datanode.opensearch.configuration.beans.OpensearchConfigurationPart;
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
        String bindAddress,
        String hostname,
        int httpPort,
        int transportPort,
        String clusterName,
        String nodeName,
        List<String> nodeRoles,
        Set<OpensearchConfigurationPart> configurationParts,

        Map<String, Object> additionalConfiguration
) {
    public Map<String, Object> asMap() {

        Map<String, Object> config = new LinkedHashMap<>();

        config.put("action.auto_create_index", "false");

        // currently, startup fails on macOS without disabling this filter.
        // for a description of the filter (although it's for ES), see https://www.elastic.co/guide/en/elasticsearch/reference/7.17/_system_call_filter_check.html
        if (OS.isFamilyMac()) {
            config.put("bootstrap.system_call_filter", "false");
        }

        if (bindAddress != null && !bindAddress.isBlank()) {
            config.put("network.host", bindAddress);
        }
        config.put("http.port", String.valueOf(httpPort));
        config.put("transport.port", String.valueOf(transportPort));
        if (clusterName != null && !clusterName.isBlank()) {
            config.put("cluster.name", clusterName);
        }

        config.put("node.name", nodeName);
        config.put("node.roles", buildRolesList());


        configurationParts.stream()
                .map(OpensearchConfigurationPart::properties)
                .forEach(config::putAll);

        config.putAll(additionalConfiguration);
        return config;
    }

    @Nonnull
    private String buildRolesList() {
        final ImmutableList.Builder<String> roles = ImmutableList.builder();
        if (nodeRoles != null) {
            roles.addAll(nodeRoles);
        }
        configurationParts.stream()
                .map(OpensearchConfigurationPart::nodeRoles)
                .forEach(roles::addAll);

        return toValuesList(roles.build());
    }

    private String toValuesList(List<String> values) {
        return String.join(",", values);
    }

    public Environment getEnv() {
        final Environment env = new Environment(System.getenv());

        List<String> javaOpts = new LinkedList<>();

        configurationParts.stream().map(OpensearchConfigurationPart::javaOpts)
                .forEach(javaOpts::addAll);

        javaOpts.add("-Dopensearch.transport.cname_in_publish_address=true");

        env.put("OPENSEARCH_JAVA_OPTS", String.join(" ", javaOpts));
        env.put("OPENSEARCH_PATH_CONF", datanodeDirectories.getOpensearchProcessConfigurationDir().toString());
        return env;
    }

    public HttpHost getRestBaseUrl() {
        final boolean sslEnabled = Boolean.parseBoolean(asMap().getOrDefault("plugins.security.ssl.http.enabled", "false").toString());
        return new HttpHost(hostname(), httpPort(), sslEnabled ? "https" : "http");
    }

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

    public Set<OpensearchConfigurationDirModifier> configDirModifiers() {
        return configurationParts.stream()
                .flatMap(c -> c.configurationDirModifiers().stream())
                .collect(Collectors.toSet());
    }
}
