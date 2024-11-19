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
package org.graylog.datanode.opensearch.cli;

import jakarta.annotation.Nonnull;
import org.assertj.core.api.Assertions;
import org.graylog.datanode.OpensearchDistribution;
import org.graylog.datanode.configuration.OpensearchArchitecture;
import org.graylog.datanode.configuration.OpensearchDistributionProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

class OpensearchKeystoreCommandLineIT {

    @Test
    void testKeystoreLifecycle(@TempDir Path tempDir) throws URISyntaxException {
        final OpensearchCli cli = createCli(tempDir);
        final String createdResponse = cli.keystore().create();

        Assertions.assertThat(createdResponse).contains("Created opensearch keystore");

        cli.keystore().add("s3.client.default.access_key", "foo");
        cli.keystore().add("s3.client.default.secret_key", "bar");

        final List<String> response = cli.keystore().list();
        Assertions.assertThat(response)
                .hasSize(3) // two keys and one internal seed
                .contains("s3.client.default.access_key")
                .contains("s3.client.default.secret_key");
    }

    private OpensearchCli createCli(Path tempDir) throws URISyntaxException {
        final Path binDirPath = detectOpensearchBinDir();
        return new OpensearchCli(tempDir, binDirPath);
    }

    @Nonnull
    private Path detectOpensearchBinDir() throws URISyntaxException {
        final Path opensearchDistRoot = Path.of(getClass().getResource("/").toURI()).getParent().resolve("opensearch");
        final OpensearchDistributionProvider distributionProvider = new OpensearchDistributionProvider(opensearchDistRoot, OpensearchArchitecture.fromOperatingSystem());
        final OpensearchDistribution opensearchDistribution = distributionProvider.get();
        return opensearchDistribution.getOpensearchBinDirPath();
    }
}
