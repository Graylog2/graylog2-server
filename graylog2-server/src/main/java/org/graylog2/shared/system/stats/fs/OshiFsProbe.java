package org.graylog2.shared.system.stats.fs;

import com.google.common.collect.ImmutableSet;
import org.graylog2.Configuration;
import org.graylog2.plugin.KafkaJournalConfiguration;
import org.graylog2.shared.system.stats.OshiService;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.common.AbstractHWDiskStore;
import oshi.software.common.AbstractOSFileStore;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import oshi.util.tuples.Pair;

import javax.inject.Inject;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class OshiFsProbe implements FsProbe {

    private final OshiService service;

    private final Set<Path> locations;
    private final Map<Path, Pair<OSFileStore, HWDiskStore>> oshiFileSystems = new HashMap<>();

    @Inject
    public OshiFsProbe(OshiService service, Configuration configuration,
                       KafkaJournalConfiguration kafkaJournalConfiguration) {
        this.service = service;

        this.locations = ImmutableSet.of(
                configuration.getBinDir(),
                configuration.getDataDir(),
                configuration.getPluginDir(),
                kafkaJournalConfiguration.getMessageJournalDir()
        );
    }

    private void init() {
        final OperatingSystem os = service.getOs();

        final FileSystem fs = os.getFileSystem();

        final HardwareAbstractionLayer hardware = service.getHal();

        for (Path location : locations) {
            Path path = location.toAbsolutePath();
            oshiFileSystems.put(location,
                    fs.getFileStores().stream()
                            .filter(it -> Paths.get(it.getMount()).startsWith(path))
                            // We want the mountpoint closest to out location
                            .max(Comparator.comparingInt(p -> Paths.get(p.getMount()).getNameCount()))
                            .map(it -> {
                                //Search for the diskstore with the partition of our mountpoint
                                return new Pair<>(it, hardware.getDiskStores().stream()
                                        .filter(ds -> ds.getPartitions().stream().anyMatch(part -> Paths.get(part.getMountPoint()).startsWith(path)))
                                        .max(Comparator.comparingInt(ds -> ds.getPartitions().stream()
                                                .filter(part -> Paths.get(part.getMountPoint()).startsWith(path))
                                                .mapToInt(part -> Paths.get(part.getMountPoint()).getNameCount())
                                                .max().orElse(0)))
                                        .orElse(generateDummyDiskStore()));
                            }).orElse(new Pair<>(generateDummyFileStore(), generateDummyDiskStore())));
        }
    }

    @Override
    public FsStats fsStats() {
        final Map<String, FsStats.Filesystem> fsmap = oshiFileSystems.entrySet().stream().peek(
                it -> {
                    it.getValue().getA().updateAttributes();
                    it.getValue().getB().updateAttributes();
                }
        ).collect(Collectors.toMap(it -> it.getKey().toString(), it -> {

                    HWDiskStore diskStore = it.getValue().getB();
                    OSFileStore fileStore = it.getValue().getA();
                    return FsStats.Filesystem.create(it.getKey().toString(), fileStore.getMount(),
                            Optional.of(fileStore.getLogicalVolume()).orElse(fileStore.getVolume()),
                            fileStore.getType(), null, fileStore.getTotalSpace(), fileStore.getUsableSpace(),
                            fileStore.getUsableSpace(), fileStore.getTotalSpace() - fileStore.getUsableSpace(),
                            (short) (((fileStore.getTotalSpace() - fileStore.getUsableSpace()) / fileStore.getTotalSpace()) * 100L),
                            fileStore.getTotalInodes(), fileStore.getFreeInodes(), fileStore.getTotalInodes() - fileStore.getFreeInodes(),
                            (short) (((fileStore.getTotalInodes() - fileStore.getFreeInodes()) / fileStore.getTotalInodes()) * 100L),
                            diskStore.getReads(),
                            diskStore.getWrites(),
                            diskStore.getReadBytes(),
                            diskStore.getWriteBytes(),
                            diskStore.getCurrentQueueLength(),
                            diskStore.getTimeStamp());
                })
        );
        return FsStats.create(fsmap);
    }

    private HWDiskStore generateDummyDiskStore() {
        return new AbstractHWDiskStore("missing", "missing", "missing", 0L) {
            @Override
            public long getReads() {
                return 0;
            }

            @Override
            public long getReadBytes() {
                return 0;
            }

            @Override
            public long getWrites() {
                return 0;
            }

            @Override
            public long getWriteBytes() {
                return 0;
            }

            @Override
            public long getCurrentQueueLength() {
                return 0;
            }

            @Override
            public long getTransferTime() {
                return 0;
            }

            @Override
            public List<HWPartition> getPartitions() {
                return null;
            }

            @Override
            public long getTimeStamp() {
                return 0;
            }

            @Override
            public boolean updateAttributes() {
                return false;
            }
        };
    }

    private OSFileStore generateDummyFileStore() {
        return new AbstractOSFileStore() {
            @Override
            public String getLogicalVolume() {
                return "missing";
            }

            @Override
            public String getDescription() {
                return "missing";
            }

            @Override
            public String getType() {
                return "dummy";
            }

            @Override
            public long getFreeSpace() {
                return 0;
            }

            @Override
            public long getUsableSpace() {
                return 0;
            }

            @Override
            public long getTotalSpace() {
                return 0;
            }

            @Override
            public long getFreeInodes() {
                return 0;
            }

            @Override
            public long getTotalInodes() {
                return 0;
            }

            @Override
            public boolean updateAttributes() {
                return false;
            }
        };
    }
}
