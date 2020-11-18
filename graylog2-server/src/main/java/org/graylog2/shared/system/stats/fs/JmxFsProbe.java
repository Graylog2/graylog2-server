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
package org.graylog2.shared.system.stats.fs;

import com.google.common.collect.ImmutableSet;
import org.graylog2.Configuration;
import org.graylog2.plugin.KafkaJournalConfiguration;

import javax.inject.Inject;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class JmxFsProbe implements FsProbe {
    private final Set<File> locations;

    @Inject
    public JmxFsProbe(Configuration configuration, KafkaJournalConfiguration kafkaJournalConfiguration) {
        this.locations = ImmutableSet.of(
                configuration.getBinDir().toFile(),
                configuration.getDataDir().toFile(),
                kafkaJournalConfiguration.getMessageJournalDir().toFile()
        );
    }

    @Override
    public FsStats fsStats() {
        final Map<String, FsStats.Filesystem> filesystems = new HashMap<>(locations.size());

        for (File location : locations) {
            final String path = location.getAbsolutePath();
            final long total = location.getTotalSpace();
            final long free = location.getFreeSpace();
            final long available = location.getUsableSpace();
            final long used = total - free;
            final short usedPercent = (short) ((double) used / total * 100);

            final FsStats.Filesystem filesystem = FsStats.Filesystem.create(
                    path, total, free, available, used, usedPercent);

            filesystems.put(path, filesystem);
        }

        return FsStats.create(filesystems);
    }
}
