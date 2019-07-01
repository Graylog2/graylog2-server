package org.graylog2.shared.system.stats.fs;

import com.google.common.collect.ImmutableSet;
import org.graylog2.Configuration;
import org.graylog2.plugin.KafkaJournalConfiguration;
import org.graylog2.shared.system.stats.OshiService;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;

import javax.inject.Inject;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class OshiFsProbe implements FsProbe {

    private final OshiService service;

    private final Set<Path> locations;
    private final Map<Path, FileSystem> oshiFileSystems = new HashMap<>();

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

    @Override
    public FsStats fsStats() {

        final OperatingSystem os = service.getOs();

        final FileSystem fs = os.getFileSystem();

        final Map<String, FsStats.Filesystem> fsmap = new HashMap<>();


        for (Path location : locations) {
            Path path = location.toAbsolutePath();

            fsmap.put(location.toString(),
                Arrays.stream(fs.getFileStores())
                    .filter(it -> Paths.get(it.getMount()).startsWith(path))
                    .max(Comparator.comparingInt(p -> Paths.get(p.getMount()).getNameCount()))
                    .map(it -> FsStats.Filesystem.create(path.toString(), it.getMount(),
                        Optional.of(it.getLogicalVolume()).orElse(it.getVolume()),
                        it.getType(), null, it.getTotalSpace(), it.getUsableSpace(),
                        it.getUsableSpace(), it.getTotalSpace() - it.getUsableSpace(),
                        (short) (((it.getTotalSpace() - it.getUsableSpace()) / it.getTotalSpace()) * 100L),
                        it.getTotalInodes(), it.getFreeInodes(), it.getTotalInodes() - it.getFreeInodes(),
                        (short) (((it.getTotalInodes() - it.getFreeInodes()) / it.getTotalInodes()) * 100L),
                        0L, 0L, 0L, 0L, 0.0, 0.0

                    )).get());
        }

        return FsStats.create(fsmap);
    }
}
