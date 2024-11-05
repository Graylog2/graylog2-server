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
package org.graylog2.cluster.nodes;

import jakarta.annotation.Nonnull;
import org.assertj.core.api.Assertions;
import org.graylog2.plugin.Version;
import org.junit.jupiter.api.Test;

class DataNodeDtoTest {

    @Test
    void testVersionCompatibility() {
        Assertions.assertThat(DataNodeDto.isVersionEqualIgnoreBuildMetadata("6.2.0-SNAPSHOT+9bec368", classpathVersion("6.2.0-SNAPSHOT")))
                .isTrue();
        Assertions.assertThat(DataNodeDto.isVersionEqualIgnoreBuildMetadata("6.2.0-SNAPSHOT+9bec368", classpathVersion("6.2.0-SNAPSHOT+9bec369")))
                .isTrue();
        Assertions.assertThat(DataNodeDto.isVersionEqualIgnoreBuildMetadata("6.2.0-SNAPSHOT+9bec368", classpathVersion("6.2.0-SNAPSHOT+9bec368")))
                .isTrue();

        Assertions.assertThat(DataNodeDto.isVersionEqualIgnoreBuildMetadata("6.2.0", classpathVersion("5.1.0")))
                .isFalse();
        Assertions.assertThat(DataNodeDto.isVersionEqualIgnoreBuildMetadata("6.1.0-SNAPSHOT+1bec368", classpathVersion("6.2.0-SNAPSHOT")))
                .isFalse();
    }

    @Nonnull
    private static Version classpathVersion(String version) {
        return new Version(com.github.zafarkhaja.semver.Version.parse(version));
    }
}
