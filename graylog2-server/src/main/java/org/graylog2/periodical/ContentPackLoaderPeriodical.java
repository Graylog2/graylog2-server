/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.periodical;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.mongodb.MongoException;
import org.graylog2.contentpacks.ContentPackLoaderConfig;
import org.graylog2.contentpacks.ContentPackPersistenceService;
import org.graylog2.contentpacks.ContentPackService;
import org.graylog2.contentpacks.model.ContentPack;
import org.graylog2.contentpacks.model.ContentPackV1;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.shared.users.UserService;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ContentPackLoaderPeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(ContentPackLoaderPeriodical.class);
    private static final HashFunction HASH_FUNCTION = Hashing.sha256();
    private static final String FILENAME_GLOB = "*.json";

    private final ObjectMapper objectMapper;
    private final ContentPackService contentPackService;
    private final ContentPackPersistenceService contentPackPersistenceService;
    private final ClusterConfigService clusterConfigService;
    private final UserService userService;
    private final boolean contentPacksLoaderEnabled;
    private final Path contentPacksDir;
    private final Set<String> contentPacksAutoLoad;

    @Inject
    public ContentPackLoaderPeriodical(ObjectMapper objectMapper,
                                       ContentPackService contentPackService,
                                       ContentPackPersistenceService contentPackPersistenceService,
                                       ClusterConfigService clusterConfigService,
                                       UserService userService,
                                       @Named("content_packs_loader_enabled") boolean contentPacksLoaderEnabled,
                                       @Named("content_packs_dir") Path contentPacksDir,
                                       @Named("content_packs_auto_load") Set<String> contentPacksAutoLoad) {
        this.objectMapper = objectMapper;
        this.contentPackService = contentPackService;
        this.contentPackPersistenceService = contentPackPersistenceService;
        this.clusterConfigService = clusterConfigService;
        this.userService = userService;
        this.contentPacksLoaderEnabled = contentPacksLoaderEnabled;
        this.contentPacksDir = contentPacksDir;
        this.contentPacksAutoLoad = ImmutableSet.copyOf(contentPacksAutoLoad);
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
        final ContentPackLoaderConfig contentPackLoaderConfig =
                clusterConfigService.getOrDefault(ContentPackLoaderConfig.class, ContentPackLoaderConfig.EMPTY);

        final List<Path> files = getFiles(contentPacksDir, FILENAME_GLOB);
        final Map<String, ContentPack> contentPacks = new HashMap<>(files.size());

        final Set<String> loadedContentPacks = new HashSet<>(contentPackLoaderConfig.loadedContentPacks());
        final Set<String> appliedContentPacks = new HashSet<>(contentPackLoaderConfig.appliedContentPacks());
        final Map<String, String> checksums = new HashMap<>(contentPackLoaderConfig.checksums());

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

            final String encodedFileName = encodeFileNameForMongo(fileName);
            final String checksum = HASH_FUNCTION.hashBytes(bytes).toString();
            final String storedChecksum = checksums.get(encodedFileName);
            if (storedChecksum == null) {
                checksums.put(encodedFileName, checksum);
            } else if (!checksum.equals(storedChecksum)) {
                LOG.info("Checksum of {} changed (expected: {}, actual: {})", file, storedChecksum, checksum);
                continue;
            }

            if (contentPackLoaderConfig.loadedContentPacks().contains(fileName)) {
                LOG.debug("Skipping already loaded content pack {} (SHA-256: {})", file, storedChecksum);
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
            loadedContentPacks.add(fileName);
        }

        LOG.debug("Applying selected content packs");
        for (Map.Entry<String, ContentPack> entry : contentPacks.entrySet()) {
            final String fileName = entry.getKey();
            final ContentPack contentPack = entry.getValue();

            if (contentPacksAutoLoad.contains(fileName) && appliedContentPacks.contains(fileName)) {
                LOG.debug("Content pack {}/{} ({}) already applied. Skipping.", contentPack.id(), contentPack.revision(), fileName);
                continue;
            }

            if (contentPacksAutoLoad.contains(fileName)) {
                LOG.debug("Applying content pack {}/{} ({})", contentPack.id(), contentPack.revision(), fileName);
                contentPackService.installContentPack(contentPack, Collections.emptyMap(), "Installed by auto loader", userService.getAdminUser().getName());
                appliedContentPacks.add(fileName);
            }
        }

        final ContentPackLoaderConfig changedContentPackLoaderConfig =
                ContentPackLoaderConfig.create(loadedContentPacks, appliedContentPacks, checksums);
        if (!contentPackLoaderConfig.equals(changedContentPackLoaderConfig)) {
            clusterConfigService.write(changedContentPackLoaderConfig);
        }
    }

    private String encodeFileNameForMongo(String fileName) {
        return fileName.replace('.', '*');
    }

    private List<Path> getFiles(final Path rootPath, final String glob) {
        final ImmutableList.Builder<Path> files = ImmutableList.builder();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(rootPath, glob)) {
            for (Path path : directoryStream) {
                if (!Files.isReadable(path)) {
                    LOG.debug("Skipping unreadable file {}", path);
                }

                if (!Files.isRegularFile(path)) {
                    LOG.debug("Path {} is not a regular file. Skipping.");
                }

                files.add(path);
            }
        } catch (IOException e) {
            LOG.warn("Couldn't list content packs: {}", e.getMessage());
        }

        return files.build();
    }
}
