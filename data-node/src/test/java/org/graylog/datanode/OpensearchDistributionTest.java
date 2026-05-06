package org.graylog.datanode;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

class OpensearchDistributionTest {
    @Test
    void testOpensearchProperties(@TempDir Path tempDir) {
        final OpensearchDistribution opensearchDistribution = new OpensearchDistribution(tempDir, "2.19.5");
        final Object snapshotsRoleName = opensearchDistribution.distributionProperties().searchableSnapshotsRole();
        Assertions.assertThat(snapshotsRoleName)
                .isEqualTo("search");
    }
}
