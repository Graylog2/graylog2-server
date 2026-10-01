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

import org.graylog.datanode.process.configuration.files.TextConfigFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The seed hosts file used by the opensearch file based discovery (discovery.seed_providers: file). Opensearch
 * re-reads this file during every discovery round, so it can be updated while the process is running.
 */
public final class UnicastHostsFile {

    public static final Path FILENAME = Path.of("unicast_hosts.txt");

    private UnicastHostsFile() {
    }

    public static TextConfigFile configFile(Collection<String> seedHosts) {
        return new TextConfigFile(FILENAME, content(seedHosts));
    }

    /**
     * Atomically replaces the seed hosts file in the given configuration directory, if its content differs.
     *
     * @return true if the file has been written, false if it already contained the given seed hosts
     */
    public static boolean update(Path configurationRoot, Collection<String> seedHosts) throws IOException {
        final Path target = configurationRoot.resolve(FILENAME);
        final String content = content(seedHosts);
        if (Files.exists(target) && Files.readString(target, StandardCharsets.UTF_8).equals(content)) {
            return false;
        }

        // opensearch may read the file anytime, write a temp file first and atomically move it in place
        final Set<PosixFilePermission> permissions = Set.of(PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_READ);
        final Path tempFile = Files.createTempFile(configurationRoot, "unicast_hosts", ".tmp", PosixFilePermissions.asFileAttribute(permissions));
        try {
            Files.writeString(tempFile, content, StandardCharsets.UTF_8);
            Files.move(tempFile, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(tempFile);
        }
        return true;
    }

    static String content(Collection<String> seedHosts) {
        return seedHosts.stream().sorted().collect(Collectors.joining("\n"));
    }
}
