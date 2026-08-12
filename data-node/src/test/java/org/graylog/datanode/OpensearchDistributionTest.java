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
package org.graylog.datanode;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

class OpensearchDistributionTest {
    @Test
    void testOpensearchCompatProperties(@TempDir Path tempDir) {
        final OpensearchDistribution dist = new OpensearchDistribution(tempDir, "2.19.5");
        Assertions.assertThat(dist.distributionProperties().searchableSnapshotsRole()).isEqualTo("search");
    }

    @Test
    void testOpensearchLatestProperties(@TempDir Path tempDir) {
        final OpensearchDistribution dist = new OpensearchDistribution(tempDir, "3.5.0");
        Assertions.assertThat(dist.distributionProperties().searchableSnapshotsRole()).isEqualTo("warm");
    }
}
