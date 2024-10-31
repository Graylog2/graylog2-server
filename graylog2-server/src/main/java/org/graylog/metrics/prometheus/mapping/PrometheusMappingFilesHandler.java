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
package org.graylog.metrics.prometheus.mapping;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import io.prometheus.client.dropwizard.samplebuilder.MapperConfig;
import org.graylog.metrics.prometheus.PrometheusExporterConfiguration;
import org.graylog2.plugin.utilities.FileInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class PrometheusMappingFilesHandler {
    private static final Logger LOG = LoggerFactory.getLogger(PrometheusMappingFilesHandler.class);
    static final String CORE_MAPPING_RESOURCE = "prometheus-exporter.yml";

    @Nullable
    private final Path coreMappingFile;
    @Nullable
    private final Path customMappingFile;
    private final String coreMappingResource;
    private final PrometheusMappingConfigLoader mapperConfigLoader;

    private final AtomicReference<FileInfo> coreMappingFileInfoRef = new AtomicReference<>(null);
    private final AtomicReference<FileInfo> customMappingFileInfoRef = new AtomicReference<>(null);

    @Inject
    public PrometheusMappingFilesHandler(@Named(PrometheusExporterConfiguration.MAPPING_FILE_PATH_CORE) @Nullable Path coreMappingPath,
                                         @Named(PrometheusExporterConfiguration.MAPPING_FILE_PATH_CUSTOM) @Nullable Path customMappingPath,
                                         PrometheusMappingConfigLoader mapperConfigLoader) {
        this(coreMappingPath, customMappingPath, CORE_MAPPING_RESOURCE, mapperConfigLoader);
    }

    @VisibleForTesting
    PrometheusMappingFilesHandler(@Named(PrometheusExporterConfiguration.MAPPING_FILE_PATH_CORE) @Nullable Path coreMappingPath,
                                  @Named(PrometheusExporterConfiguration.MAPPING_FILE_PATH_CUSTOM) @Nullable Path customMappingPath,
                                  String coreMappingResource,
                                  PrometheusMappingConfigLoader mapperConfigLoader) {
        this.coreMappingFile = buildPath(coreMappingPath, coreMappingFileInfoRef);
        this.customMappingFile = buildPath(customMappingPath, customMappingFileInfoRef);
        this.coreMappingResource = coreMappingResource;
        this.mapperConfigLoader = mapperConfigLoader;
    }

    private static Path buildPath(Path file, AtomicReference<FileInfo> fileInfoRef) {
        if (file != null) {
            if (Files.exists(file)) {
                fileInfoRef.set(FileInfo.forPath(file));
            }
            return file.toAbsolutePath();
        }
        return null;
    }

    private Optional<FileInfo.Change> detectChange(@Nullable Path file, AtomicReference<FileInfo> fileInfoRef) {
        if (file != null && Files.exists(file)) {
            LOG.debug("Detecting changes for file <{}>", file);

            if (fileInfoRef.get() == null) {
                LOG.debug("Getting initial file info for file <{}>", file);
                final FileInfo newFileInfo = FileInfo.forPath(file);
                fileInfoRef.set(newFileInfo);
                // The file didn't exist before so we want to trigger a change
                return Optional.of(new FileInfo.Change(newFileInfo));
            }

            final FileInfo.Change change = fileInfoRef.get().checkForChange();

            if (change.isChanged()) {
                LOG.debug("Updating file info for changed file <{}>", file);
                // Use latest file info to make update check correct on next call
                fileInfoRef.set(change.fileInfo());
            }

            return Optional.of(change);
        }

        LOG.debug("File <{}> doesn't exist", file);
        return Optional.empty();
    }

    public boolean filesHaveChanged() {
        if ((coreMappingFile == null || !Files.exists(coreMappingFile)) &&
                (customMappingFile == null || !Files.exists(customMappingFile))) {
            return false;
        }

        final List<FileInfo.Change> changes = new ArrayList<>();

        // Synchronize to ensure atomic update of the file info fields
        synchronized (this) {
            detectChange(coreMappingFile, coreMappingFileInfoRef).ifPresent(changes::add);
            detectChange(customMappingFile, customMappingFileInfoRef).ifPresent(changes::add);
        }

        return changes.stream().anyMatch(FileInfo.Change::isChanged);
    }

    public List<MapperConfig> getMapperConfigs() {
        final Set<MapperConfig> mapperConfigs = new HashSet<>();

        // If an external core mapping file exists it takes precedence over the included mapping resource
        if (coreMappingFile != null && Files.exists(coreMappingFile)) {
            LOG.debug("Loading core metric mappings from file <{}>", coreMappingFile);
            try {
                final InputStream inputStream = Files.newInputStream(coreMappingFile, StandardOpenOption.READ);
                mapperConfigs.addAll(mapperConfigLoader.load(inputStream));
            } catch (IOException e) {
                LOG.error("Couldn't load mapping from file <{}>", coreMappingFile, e);
            }
        } else {
            LOG.debug("Loading core metric mappings from resource <{}>", coreMappingResource);
            try {
                mapperConfigs.addAll(mapperConfigLoader.load(Resources.getResource(coreMappingResource).openStream()));
            } catch (IOException e) {
                LOG.error("Couldn't load mapping from resource <{}>", coreMappingResource, e);
            }
        }

        // Load custom mappings if they exist. Custom mappings cannot override core mappings!
        if (customMappingFile != null && Files.exists(customMappingFile)) {
            LOG.debug("Loading custom metric mappings from file <{}>", customMappingFile);
            try {
                final Set<String> coreMetricNames = mapperConfigs.stream()
                        .map(MapperConfig::getName)
                        .collect(Collectors.toSet());

                final InputStream inputStream = Files.newInputStream(customMappingFile, StandardOpenOption.READ);
                final Set<MapperConfig> customConfigs = mapperConfigLoader.load(inputStream)
                        .stream()
                        .filter(config -> {
                            if (coreMetricNames.contains(config.getName())) {
                                LOG.warn("Custom metric mapping config cannot overwrite core metric: {}", config.getName());
                                return false;
                            }
                            return true;
                        })
                        .collect(Collectors.toSet());

                mapperConfigs.addAll(customConfigs);
            } catch (IOException e) {
                LOG.error("Couldn't load mapping from file <{}>", coreMappingFile, e);
            }
        }

        return ImmutableList.copyOf(mapperConfigs);
    }
}
