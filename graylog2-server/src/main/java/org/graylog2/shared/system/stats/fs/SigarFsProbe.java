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
import org.graylog2.shared.system.stats.SigarService;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemMap;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SigarFsProbe implements FsProbe {
    private final SigarService sigarService;
    private final Set<Path> locations;
    private final Map<Path, FileSystem> sigarFileSystems = new HashMap<>();

    @Inject
    public SigarFsProbe(SigarService sigarService, Configuration configuration,
                        KafkaJournalConfiguration kafkaJournalConfiguration) {
        this.sigarService = sigarService;
        this.locations = ImmutableSet.of(
                configuration.getBinDir(),
                configuration.getDataDir(),
                configuration.getPluginDir(),
                kafkaJournalConfiguration.getMessageJournalDir()
        );
    }

    @Override
    public synchronized FsStats fsStats() {
        final Sigar sigar = sigarService.sigar();
        final Map<String, FsStats.Filesystem> filesystems = new HashMap<>(locations.size());

        for (Path location : locations) {
            final String path = location.toAbsolutePath().toString();

            try {
                FileSystem fileSystem = sigarFileSystems.get(location);

                if (fileSystem == null) {
                    FileSystemMap fileSystemMap = sigar.getFileSystemMap();
                    if (fileSystemMap != null) {
                        fileSystem = fileSystemMap.getMountPoint(path);
                        sigarFileSystems.put(location, fileSystem);
                    }
                }

                String mount = null;
                String dev = null;
                String typeName = null;
                String sysTypeName = null;
                long total = -1;
                long free = -1;
                long available = -1;
                long used = -1;
                short usedPercent = -1;
                long inodesTotal = -1;
                long inodesFree = -1;
                long inodesUsed = -1;
                short inodesUsedPercent = -1;
                long diskReads = -1;
                long diskWrites = -1;
                long diskReadBytes = -1;
                long diskWriteBytes = -1;
                double diskQueue = -1.0d;
                double diskServiceTime = -1.0d;
                if (fileSystem != null) {
                    mount = fileSystem.getDirName();
                    dev = fileSystem.getDevName();
                    typeName = fileSystem.getTypeName();
                    sysTypeName = fileSystem.getSysTypeName();

                    final FileSystemUsage fileSystemUsage = sigar.getFileSystemUsage(mount);
                    if (fileSystemUsage != null) {
                        total = fileSystemUsage.getTotal() * 1024;
                        free = fileSystemUsage.getFree() * 1024;
                        available = fileSystemUsage.getAvail() * 1024;
                        used = fileSystemUsage.getUsed() * 1024;
                        usedPercent = (short) (fileSystemUsage.getUsePercent() * 100);

                        inodesTotal = fileSystemUsage.getFiles();
                        inodesFree = fileSystemUsage.getFreeFiles();
                        inodesUsed = inodesTotal - inodesFree;
                        inodesUsedPercent = (short) ((double) inodesUsed / inodesTotal * 100);

                        diskReads = fileSystemUsage.getDiskReads();
                        diskWrites = fileSystemUsage.getDiskWrites();
                        diskReadBytes = fileSystemUsage.getDiskReadBytes();
                        diskWriteBytes = fileSystemUsage.getDiskWriteBytes();
                        diskQueue = fileSystemUsage.getDiskQueue();
                        diskServiceTime = fileSystemUsage.getDiskServiceTime();
                    }
                }

                final FsStats.Filesystem filesystem = FsStats.Filesystem.create(
                        path, mount, dev, typeName, sysTypeName, total, free, available, used, usedPercent,
                        inodesTotal, inodesFree, inodesUsed, inodesUsedPercent,
                        diskReads, diskWrites, diskReadBytes, diskWriteBytes, diskQueue, diskServiceTime
                );
                filesystems.put(path, filesystem);
            } catch (SigarException e) {
                // ignore
            }
        }

        return FsStats.create(filesystems);
    }
}
