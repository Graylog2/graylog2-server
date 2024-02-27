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
package org.graylog2.rest.resources.system;

import jakarta.ws.rs.BadRequestException;
import org.glassfish.jersey.message.internal.FileProvider;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.validate.ClusterConfigValidatorService;
import org.graylog2.security.RestrictedChainingClassLoader;
import org.graylog2.security.SafeClasses;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.plugins.ChainingClassLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.graylog2.shared.utilities.StringUtils.f;

@ExtendWith(MockitoExtension.class)
class ClusterConfigResourceTest {
    @Mock
    private ClusterConfigService clusterConfigService;

    @Mock
    private ClusterConfigValidatorService clusterConfigValidatorService;

    @Test
    void putClassConsideredUnsafe(@TempDir Path tmpDir) throws IOException {
        final Path file = tmpDir.resolve("secrets.txt");
        Files.writeString(file, "secret content");

        final ClusterConfigResource resource = new ClusterConfigResource(clusterConfigService,
                new RestrictedChainingClassLoader(new ChainingClassLoader(this.getClass().getClassLoader()),
                        SafeClasses.allGraylogInternal()),
                new ObjectMapperProvider().get(),
                clusterConfigValidatorService
        );

        assertThatThrownBy(() -> resource.update("java.io.File",
                new ByteArrayInputStream(f("\"%s\"", file.toAbsolutePath()).getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Prevented loading of unsafe class");
    }

    /**
     * Proof of concept to show what would happen if we'd allow problematic classes
     */
    @Test
    void putClassConsideredSafe(@TempDir Path tmpDir) throws IOException {
        final Path file = tmpDir.resolve("secrets.txt");
        Files.writeString(file, "secret content");

        final ClusterConfigResource resource = new ClusterConfigResource(clusterConfigService,
                new RestrictedChainingClassLoader(new ChainingClassLoader(this.getClass().getClassLoader()),
                        new SafeClasses(Set.of("java.io.File"))),
                new ObjectMapperProvider().get(),
                clusterConfigValidatorService);

        // Simulate how jersey would serialize a File object
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        new FileProvider().writeTo((File) resource.update("java.io.File",
                new ByteArrayInputStream(f("\"%s\"", file.toAbsolutePath()).getBytes(StandardCharsets.UTF_8))).getEntity(), null, null, null, null, null, out);
        final String content = out.toString(StandardCharsets.UTF_8);

        assertThat(content).isEqualTo("secret content");
    }
}
