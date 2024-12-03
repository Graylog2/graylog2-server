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
import org.graylog.datanode.configuration.variants.OpensearchSecurityConfiguration;
import org.graylog.datanode.opensearch.configuration.beans.OpensearchConfigurationPart;
import org.graylog.datanode.process.Environment;
import org.graylog.shaded.opensearch2.org.apache.http.HttpHost;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        List<String> discoverySeedHosts,
        OpensearchSecurityConfiguration opensearchSecurityConfiguration,
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

        if (discoverySeedHosts != null && !discoverySeedHosts.isEmpty()) {
            config.put("discovery.seed_hosts", toValuesList(discoverySeedHosts));
        }

        config.put("discovery.seed_providers", "file");

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
        javaOpts.add("-Xms%s".formatted(opensearchSecurityConfiguration.getOpensearchHeap()));
        javaOpts.add("-Xmx%s".formatted(opensearchSecurityConfiguration.getOpensearchHeap()));
        javaOpts.add("-Dopensearch.transport.cname_in_publish_address=true");

        opensearchSecurityConfiguration.getTruststore().ifPresent(truststore -> {
            javaOpts.add("-Djavax.net.ssl.trustStore=" + truststore.location().toAbsolutePath());
            javaOpts.add("-Djavax.net.ssl.trustStorePassword=" + new String(truststore.password()));
            javaOpts.add("-Djavax.net.ssl.trustStoreType=pkcs12");
        });

        env.put("OPENSEARCH_JAVA_OPTS", String.join(" ", javaOpts));
        env.put("OPENSEARCH_PATH_CONF", datanodeDirectories.getOpensearchProcessConfigurationDir().toString());
        return env;
    }

    public HttpHost getRestBaseUrl() {
        final boolean sslEnabled = Boolean.parseBoolean(asMap().getOrDefault("plugins.security.ssl.http.enabled", "false").toString());
        return new HttpHost(hostname(), httpPort(), sslEnabled ? "https" : "http");
    }

    public HttpHost getClusterBaseUrl() {
        final boolean sslEnabled = Boolean.parseBoolean(asMap().getOrDefault("plugins.security.ssl.http.enabled", "false").toString());
        return new HttpHost(hostname(), transportPort(), sslEnabled ? "https" : "http");
    }

    public boolean securityConfigured() {
        return opensearchSecurityConfiguration() != null;
    }


    public Map<String, String> getKeystoreItems() {

        final ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        builder.putAll(opensearchSecurityConfiguration.getKeystoreItems());

        configurationParts.stream()
                .map(OpensearchConfigurationPart::keystoreItems)
                .forEach(builder::putAll);

        return builder.build();
    }
}
