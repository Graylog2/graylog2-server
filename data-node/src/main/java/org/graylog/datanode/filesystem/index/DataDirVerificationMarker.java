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
package org.graylog.datanode.filesystem.index;

import com.github.zafarkhaja.semver.Version;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * A small file inside the opensearch data directory recording which opensearch version last opened it successfully.
 * <p>
 * Deliberately stored next to the data rather than in the datanode metadata in mongodb: the claim is a property of
 * the <em>directory</em>, not of the node. Metadata is keyed by node id, so it would keep asserting that a major
 * version has opened a directory even after an administrator repointed {@code opensearch_data_location} somewhere
 * else — and that assertion is used to skip a safety check. A missing marker only costs us a re-scan; a marker that
 * describes the wrong directory would let us start an indexer that cannot read the data.
 * <p>
 * The marker is written once opensearch has actually reached a running state, so it records proof rather than a
 * prediction. Readers only ever compare major versions: an index format is readable by the release that wrote it and
 * the one following it, so "major N opened this directory, and only major N has written to it since" is enough to
 * conclude that major N can open it again.
 */
@Singleton
public class DataDirVerificationMarker {

    private static final Logger LOG = LoggerFactory.getLogger(DataDirVerificationMarker.class);

    static final String FILENAME = ".dn-compat-check";

    /**
     * @return the major version of the opensearch release that last opened this directory, empty if the marker is
     * absent or unusable. Empty always means "we don't know", never "incompatible".
     */
    public Optional<Long> verifiedMajorVersion(Path dataDir) {
        final Path markerFile = dataDir.resolve(FILENAME);
        if (!Files.exists(markerFile)) {
            return Optional.empty();
        }
        try {
            final String storedVersion = Files.readString(markerFile, StandardCharsets.UTF_8).trim();
            return Optional.of(Version.parse(storedVersion).majorVersion());
        } catch (Exception e) {
            LOG.warn("Failed to read the opensearch data directory marker file {}, treating it as absent", markerFile, e);
            return Optional.empty();
        }
    }

    public boolean isVerifiedFor(Path dataDir, String opensearchVersion) {
        try {
            final long major = Version.parse(opensearchVersion).majorVersion();
            return verifiedMajorVersion(dataDir).filter(verified -> verified == major).isPresent();
        } catch (Exception e) {
            LOG.warn("Failed to parse opensearch version {}", opensearchVersion, e);
            return false;
        }
    }

    /**
     * Records that {@code opensearchVersion} has successfully opened this directory. Failures are logged and
     * swallowed: losing the marker only means the next startup does the full scan again.
     */
    public void record(Path dataDir, String opensearchVersion) {
        final Path markerFile = dataDir.resolve(FILENAME);
        try {
            Files.writeString(markerFile, opensearchVersion, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.warn("Failed to write the opensearch data directory marker file {}, the directory will be scanned "
                    + "again on the next startup", markerFile, e);
        }
    }
}
