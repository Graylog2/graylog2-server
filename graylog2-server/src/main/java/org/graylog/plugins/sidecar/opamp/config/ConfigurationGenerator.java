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
package org.graylog.plugins.sidecar.opamp.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.plugins.sidecar.rest.models.Collector;
import org.graylog.plugins.sidecar.rest.models.Configuration;
import org.graylog.plugins.sidecar.rest.models.Sidecar;
import org.graylog.plugins.sidecar.rest.requests.ConfigurationAssignment;
import org.graylog.plugins.sidecar.services.CollectorService;
import org.graylog.plugins.sidecar.services.ConfigurationService;
import org.graylog.plugins.sidecar.template.RenderTemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates OTel Collector configuration YAML for OpAMP agents based on
 * the sidecar's assigned configurations.
 */
@Singleton
public class ConfigurationGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationGenerator.class);

    private final ConfigurationService configurationService;
    private final CollectorService collectorService;
    private final YAMLMapper yamlMapper;

    @Inject
    public ConfigurationGenerator(ConfigurationService configurationService,
                                  CollectorService collectorService,
                                  YAMLMapper yamlMapper) {
        this.configurationService = configurationService;
        this.collectorService = collectorService;
        this.yamlMapper = yamlMapper;
    }

    /**
     * Generates the OTel Collector configuration YAML for the given sidecar.
     *
     * @param sidecar the sidecar to generate configuration for
     * @return the YAML configuration as a string
     */
    public String generateConfig(Sidecar sidecar) {
        final List<Map<String, Object>> collectors = new ArrayList<>();

        for (ConfigurationAssignment assignment : sidecar.assignments()) {
            try {
                final Map<String, Object> collectorConfig = buildCollectorConfig(sidecar, assignment);
                if (collectorConfig != null) {
                    collectors.add(collectorConfig);
                }
            } catch (Exception e) {
                LOG.error("Failed to build config for assignment {}: {}",
                        assignment.configurationId(), e.getMessage());
            }
        }

        // Build the full config structure
        final Map<String, Object> sidecarExtension = new LinkedHashMap<>();
        sidecarExtension.put("collectors", collectors);

        final Map<String, Object> extensions = new LinkedHashMap<>();
        extensions.put("sidecar", sidecarExtension);

        final Map<String, Object> config = new LinkedHashMap<>();
        config.put("extensions", extensions);

        try {
            return yamlMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            LOG.error("Failed to serialize config to YAML", e);
            return "";
        }
    }

    /**
     * Computes the SHA-256 hash of the configuration for the given sidecar.
     *
     * @param sidecar the sidecar to compute config hash for
     * @return the SHA-256 hash as a byte array
     */
    public record GeneratedConfig(String yaml, byte[] hash) {}

    public GeneratedConfig generateConfigWithHash(Sidecar sidecar) {
        final String yaml = generateConfig(sidecar);
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hash = digest.digest(yaml.getBytes(StandardCharsets.UTF_8));
            return new GeneratedConfig(yaml, hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Object> buildCollectorConfig(Sidecar sidecar, ConfigurationAssignment assignment) {
        final Configuration configuration = configurationService.find(assignment.configurationId());
        if (configuration == null) {
            LOG.warn("Configuration not found: {}", assignment.configurationId());
            return null;
        }

        final Collector collector = collectorService.find(assignment.collectorId());
        if (collector == null) {
            LOG.warn("Collector not found: {}", assignment.collectorId());
            return null;
        }

        // Render the configuration template with sidecar context
        String renderedConfig;
        try {
            final Configuration rendered = configurationService.renderConfigurationForCollector(sidecar, configuration);
            renderedConfig = rendered.template();
        } catch (RenderTemplateException e) {
            LOG.error("Failed to render configuration template {}: {}", configuration.id(), e.getMessage());
            renderedConfig = configuration.template();
        }

        // Build the collector configuration map
        final Map<String, Object> collectorConfig = new LinkedHashMap<>();
        collectorConfig.put("id", configuration.id());
        collectorConfig.put("name", collector.name());
        collectorConfig.put("service_type", collector.serviceType());
        collectorConfig.put("executable_path", collector.executablePath());

        if (collector.executeParameters() != null) {
            collectorConfig.put("execute_parameters", collector.executeParameters());
        }
        if (collector.validationParameters() != null) {
            collectorConfig.put("validation_parameters", collector.validationParameters());
        }

        // Generate configuration path based on sidecar's config directory
        final boolean isWindows = sidecar.nodeDetails().operatingSystem()
                .equalsIgnoreCase("windows");
        final String pathSeparator = isWindows ? "\\" : "/";

        // Use sidecar's config directory, or provide sensible OS-specific default
        String configDir = sidecar.nodeDetails().collectorConfigurationDirectory();
        if (configDir == null || configDir.isEmpty()) {
            configDir = isWindows
                    ? "C:\\Program Files\\Graylog\\sidecar\\generated"
                    : "/var/lib/graylog-sidecar/generated";
        }

        collectorConfig.put("configuration_path",
                configDir + pathSeparator + collector.name() + "-" + configuration.id() + ".yml");

        collectorConfig.put("configuration", renderedConfig);

        return collectorConfig;
    }
}
