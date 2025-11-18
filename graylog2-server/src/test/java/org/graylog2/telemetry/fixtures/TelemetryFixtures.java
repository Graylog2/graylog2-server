package org.graylog2.telemetry.fixtures;

import org.graylog2.telemetry.cluster.db.TelemetryClusterInfoDto;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Random;

public class TelemetryFixtures {
    private static final Random RNG = new Random();

    public static TelemetryClusterInfoDto nodeInfo(String nodeId, boolean isLeader) {
        final long randLong = RNG.nextLong(1_000_000);
        final int randInt = RNG.nextInt(20);

        return TelemetryClusterInfoDto.Builder.create()
                .nodeId(nodeId)
                .isLeader(isLeader)
                .clusterId("cluster-" + randLong)
                .codename("Noir-" + randLong)
                .facility("graylog-server-" + randLong)
                .hostname("hostname-" + randLong)
                .isProcessing(true)
                .lbStatus("active")
                .lifecycle("running")
                .operatingSystem("Linux 5.4-" + randLong)
                .startedAt(DateTime.now(DateTimeZone.UTC).minusDays(5))
                .timezone("UTC-" + randLong)
                .version("5.2.0-" + randLong)
                .memoryHeapUsed(randLong)
                .memoryHeapCommitted(randLong)
                .memoryHeapMax(randLong)
                .cpuCores(randInt)
                .updatedAt(DateTime.now(DateTimeZone.UTC))
                .build();
    }
}
