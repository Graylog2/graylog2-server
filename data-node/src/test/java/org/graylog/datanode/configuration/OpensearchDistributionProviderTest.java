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
package org.graylog.datanode.configuration;

import org.assertj.core.api.Assertions;
import org.graylog.datanode.OpensearchDistribution;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

class OpensearchDistributionProviderTest {

    @TempDir
    private Path tempDir;

    @TempDir
    private Path emptyTempDir;

    @TempDir
    private Path tempDirWithoutArch;

    @TempDir
    private Path multiVersionDir;

    @BeforeEach
    void setUp() throws IOException {
        Files.createDirectory(tempDir.resolve("opensearch-2.5.0-linux-x64"));
        Files.createDirectory(tempDir.resolve("opensearch-2.5.0-linux-aarch64"));
        Files.createDirectory(tempDir.resolve("somethingelse")); // just to include something which is not OS dist

        Files.createDirectory(tempDirWithoutArch.resolve("opensearch-2.4.1"));
        Files.createDirectory(tempDir.resolve(".config")); // just to include something which is not OS dist

        Files.createDirectory(multiVersionDir.resolve("opensearch-2.19.5-linux-x64"));
        Files.createDirectory(multiVersionDir.resolve("opensearch-2.19.5-linux-aarch64"));
        Files.createDirectory(multiVersionDir.resolve("opensearch-3.5.0-linux-x64"));
        Files.createDirectory(multiVersionDir.resolve("opensearch-3.5.0-linux-aarch64"));
    }

    @Test
    void testFailedDetectionInDirectory() {
        Assertions.assertThatThrownBy(() ->
                        provider(emptyTempDir.resolve("nonexistent"), OpensearchArchitecture.x64).get())
                .hasMessageStartingWith("Failed to list content of provided directory");


        Assertions.assertThatThrownBy(() -> provider(emptyTempDir, OpensearchArchitecture.x64).get())
                .hasMessageStartingWith("Could not detect any opensearch distribution");
    }

    @Test
    void testDetection() {
        final OpensearchDistribution x64 = provider(tempDir, OpensearchArchitecture.x64).get();
        Assertions.assertThat(x64.version()).isEqualTo("2.5.0");
        Assertions.assertThat(x64.platform()).isEqualTo("linux");
        Assertions.assertThat(x64.architecture()).isEqualTo(OpensearchArchitecture.x64);

        final OpensearchDistribution mac = provider(tempDir, OpensearchArchitecture.x64).get();
        Assertions.assertThat(mac.version()).isEqualTo("2.5.0");
        Assertions.assertThat(mac.platform()).isEqualTo("linux");
        Assertions.assertThat(mac.architecture()).isEqualTo(OpensearchArchitecture.x64);

        final OpensearchDistribution aarch64 = provider(tempDir, OpensearchArchitecture.aarch64).get();
        Assertions.assertThat(aarch64.version()).isEqualTo("2.5.0");
        Assertions.assertThat(aarch64.platform()).isEqualTo("linux");
        Assertions.assertThat(aarch64.architecture()).isEqualTo(OpensearchArchitecture.aarch64);
    }

    @Test
    void testDetectionWithoutArch() {
        final OpensearchDistribution dist = provider(tempDirWithoutArch, OpensearchArchitecture.x64).get();
        Assertions.assertThat(dist.version()).isEqualTo("2.4.1");
        Assertions.assertThat(dist.architecture()).isNull();
        Assertions.assertThat(dist.platform()).isNull();
    }

    @Test
    void testBackwardsCompatibility() {
        // we are not pointing to the root directory which should contain different OS distributions but rather directly to one
        // specific distribution.
        final OpensearchDistribution dist = provider(tempDir.resolve("opensearch-2.5.0-linux-x64"), OpensearchArchitecture.x64).get();
        Assertions.assertThat(dist.version()).isEqualTo("2.5.0");
        Assertions.assertThat(dist.platform()).isEqualTo("linux");
        Assertions.assertThat(dist.architecture()).isEqualTo(OpensearchArchitecture.x64);
    }

    @Test
    void testVersionSelectionDefaultsToOldest() {
        final OpensearchDistribution x64 = provider(multiVersionDir, OpensearchArchitecture.x64).get();
        Assertions.assertThat(x64.version()).isEqualTo("2.19.5");
        Assertions.assertThat(x64.architecture()).isEqualTo(OpensearchArchitecture.x64);

        final OpensearchDistribution aarch64 = provider(multiVersionDir, OpensearchArchitecture.aarch64).get();
        Assertions.assertThat(aarch64.version()).isEqualTo("2.19.5");
        Assertions.assertThat(aarch64.architecture()).isEqualTo(OpensearchArchitecture.aarch64);
    }

    @Test
    void testVersionSelectionByExplicitVersion() {
        final OpensearchDistribution v2 = providerWithVersion(multiVersionDir, OpensearchArchitecture.x64, "2.19.5").get();
        Assertions.assertThat(v2.version()).isEqualTo("2.19.5");
        Assertions.assertThat(v2.architecture()).isEqualTo(OpensearchArchitecture.x64);

        final OpensearchDistribution v3 = providerWithVersion(multiVersionDir, OpensearchArchitecture.x64, "3.5.0").get();
        Assertions.assertThat(v3.version()).isEqualTo("3.5.0");
        Assertions.assertThat(v3.architecture()).isEqualTo(OpensearchArchitecture.x64);
    }

    @Test
    void testVersionSelectionFailsForUnknownVersion() {
        Assertions.assertThatThrownBy(() ->
                        providerWithVersion(multiVersionDir, OpensearchArchitecture.x64, "9.9.9").get())
                .hasMessageContaining("No OpenSearch distribution found for requested version '9.9.9'")
                .hasMessageContaining("2.19.5")
                .hasMessageContaining("3.5.0");
    }

    @Nonnull
    private OpensearchDistributionProvider provider(Path dir, OpensearchArchitecture arch) {
        return new OpensearchDistributionProvider(dir, arch);
    }

    @Nonnull
    private OpensearchDistributionProvider providerWithVersion(Path dir, OpensearchArchitecture arch, String version) {
        return new OpensearchDistributionProvider(dir, arch, Optional.of(version));
    }
}
