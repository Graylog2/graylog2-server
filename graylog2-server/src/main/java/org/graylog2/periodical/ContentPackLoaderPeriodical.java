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
package org.graylog2.periodical;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.graylog2.Configuration;
import org.graylog2.contentpacks.ContentPackInstallationPersistenceService;
import org.graylog2.contentpacks.ContentPackPersistenceService;
import org.graylog2.contentpacks.ContentPackService;
import org.graylog2.contentpacks.model.ContentPack;
import org.graylog2.contentpacks.model.ContentPackV1;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ContentPackLoaderPeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(ContentPackLoaderPeriodical.class);
    private static final String FILENAME_GLOB = "*.json";

    private final ObjectMapper objectMapper;
    private final ContentPackService contentPackService;
    private final ContentPackPersistenceService contentPackPersistenceService;
    private final ContentPackInstallationPersistenceService contentPackInstallationPersistenceService;
    private final Configuration configuration;
    private final boolean contentPacksLoaderEnabled;
    private final Path contentPacksDir;
    private final Set<String> contentPacksAutoInstall;

    @Inject
    public ContentPackLoaderPeriodical(ObjectMapper objectMapper,
                                       ContentPackService contentPackService,
                                       ContentPackPersistenceService contentPackPersistenceService,
                                       ContentPackInstallationPersistenceService contentPackInstallationPersistenceService,
                                       Configuration configuration,
                                       @Named("content_packs_loader_enabled") boolean contentPacksLoaderEnabled,
                                       @Named("content_packs_dir") Path contentPacksDir,
                                       @Named("content_packs_auto_install") Set<String> contentPacksAutoInstall) {
        this.objectMapper = objectMapper;
        this.contentPackInstallationPersistenceService = contentPackInstallationPersistenceService;
        this.contentPackService = contentPackService;
        this.contentPackPersistenceService = contentPackPersistenceService;
        this.configuration = configuration;
        this.contentPacksLoaderEnabled = contentPacksLoaderEnabled;
        this.contentPacksDir = contentPacksDir;
        this.contentPacksAutoInstall = ImmutableSet.copyOf(contentPacksAutoInstall);
    }

    @Override
    public boolean runsForever() {
        return true;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return false;
    }

    @Override
    public boolean masterOnly() {
        return true;
    }

    @Override
    public boolean startOnThisNode() {
        return contentPacksLoaderEnabled;
    }

    @Override
    public boolean isDaemon() {
        return true;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return 0;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public void doRun() {
        if (!Files.exists(contentPacksDir.toAbsolutePath())) {
           LOG.warn("Could not find content packs directory {}. Please check your graylog configuration",
                   contentPacksDir.toAbsolutePath());
           return;
        }

        final List<Path> files = getFiles(contentPacksDir);
        final Map<String, ContentPack> contentPacks = new HashMap<>(files.size());

        for (Path file : files) {
            final String fileName = file.getFileName().toString();

            LOG.debug("Reading content pack from {}", file);
            final byte[] bytes;
            try {
                bytes = Files.readAllBytes(file);
            } catch (IOException e) {
                LOG.warn("Couldn't read " + file + ". Skipping.", e);
                continue;
            }

            LOG.debug("Parsing content pack from {}", file);
            final ContentPack contentPack;
            try {
                contentPack = objectMapper.readValue(bytes, ContentPack.class);
            } catch (IOException e) {
                LOG.warn("Couldn't parse content pack in file " + file + ". Skipping", e);
                continue;
            }

            if (contentPack instanceof ContentPackV1) {
                ContentPackV1 contentPackV1 = (ContentPackV1) contentPack;
                if (contentPackV1.parameters().asList().size() > 0) {
                    LOG.warn("Cannot accept content packs with parameters. Content Pack {}/{} rejected", contentPack.id(),
                            contentPack.revision());
                }
            }

            final Optional<ContentPack> existingContentPack = contentPackPersistenceService.findByIdAndRevision(contentPack.id(), contentPack.revision());
            if (existingContentPack.isPresent()) {
                LOG.debug("Content pack {}/{} already exists in database. Skipping.", contentPack.id(), contentPack.revision());
                contentPacks.put(fileName, existingContentPack.get());
                continue;
            }

            final Optional<ContentPack> insertedContentPack = contentPackPersistenceService.insert(contentPack);
            if (!insertedContentPack.isPresent()) {
                LOG.error("Error while inserting content pack " + file + " into database. Skipping.");
                continue;
            }

            contentPacks.put(fileName, insertedContentPack.get());
        }

        LOG.debug("Applying selected content packs");
        for (Map.Entry<String, ContentPack> entry : contentPacks.entrySet()) {
            final String fileName = entry.getKey();
            final ContentPack contentPack = entry.getValue();

            if (contentPacksAutoInstall.contains(fileName)) {
                if (!contentPackInstallationPersistenceService.
                        findByContentPackIdAndRevision(contentPack.id(),contentPack.revision()).isEmpty()) {
                    LOG.debug("Content pack {}/{} ({}) already applied. Skipping.", contentPack.id(), contentPack.revision(), fileName);
                    continue;
                }

                contentPackService.installContentPack(contentPack, Collections.emptyMap(),
                        "Installed by auto loader", configuration.getRootUsername());
            }

        }
    }

    private List<Path> getFiles(final Path rootPath) {
        final ImmutableList.Builder<Path> files = ImmutableList.builder();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(rootPath, ContentPackLoaderPeriodical.FILENAME_GLOB)) {
            for (Path path : directoryStream) {
                if (!Files.isReadable(path)) {
                    LOG.debug("Skipping unreadable file {}", path);
                }

                if (!Files.isRegularFile(path)) {
                    LOG.debug("Path {} is not a regular file. Skipping.", path);
                }

                files.add(path);
            }
        } catch (IOException e) {
            LOG.warn("Couldn't list content packs: {}", e.getMessage());
        }

        return files.build();
    }
}
