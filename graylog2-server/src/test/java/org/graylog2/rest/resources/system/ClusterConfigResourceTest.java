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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import org.apache.shiro.subject.Subject;
import org.glassfish.jersey.message.internal.FileProvider;
import org.graylog2.cluster.ClusterConfigChangedEvent;
import org.graylog2.configuration.tunables.CoreRuntimeTunableFeatures;
import org.graylog2.configuration.tunables.RuntimeTunable;
import org.graylog2.configuration.tunables.RuntimeTunableDescriptorFactory;
import org.graylog2.configuration.tunables.RuntimeTunableRegistry;
import org.graylog2.configuration.tunables.RuntimeTunableSchemaGenerator;
import org.graylog2.configuration.tunables.RuntimeTunableSecrets;
import org.graylog2.configuration.tunables.RuntimeTunableService;
import org.graylog2.configuration.tunables.TunableValidator;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.rest.ValidationFailureException;
import org.graylog2.plugin.system.NodeId;
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
import org.mockito.quality.Strictness;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.graylog2.shared.utilities.StringUtils.f;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
class ClusterConfigResourceTest {
    @Mock
    private ClusterConfigService clusterConfigService;

    @Mock
    private ClusterConfigValidatorService clusterConfigValidatorService;

    @Mock
    private ClusterEventBus clusterEventBus;

    @Mock
    private NodeId nodeId;

    @Mock
    private RuntimeTunableService runtimeTunableService;

    @RuntimeTunable(id = "test.legacy", feature = "search", title = "Legacy")
    public record LegacyTunable(@JsonProperty("limit") @Min(1) int limit) {
        public static LegacyTunable defaults() {
            return new LegacyTunable(5);
        }
    }

    private static RuntimeTunableDescriptorFactory factory(ObjectMapper objectMapper) {
        return new RuntimeTunableDescriptorFactory(new RuntimeTunableSchemaGenerator(objectMapper),
                new TunableValidator(objectMapper, new ClusterConfigValidatorService(Map.of())), objectMapper);
    }

    private static RuntimeTunableRegistry emptyRegistry() {
        return new RuntimeTunableRegistry(Set.of(), Set.of(), factory(new ObjectMapperProvider().get()));
    }

    @RuntimeTunable(id = "test.restricted_legacy", feature = "search", title = "Restricted legacy",
            readPermission = "legacy:read", editPermission = "legacy:edit")
    public record RestrictedLegacyTunable(@JsonProperty("limit") @Min(1) int limit) {
        public static RestrictedLegacyTunable defaults() {
            return new RestrictedLegacyTunable(5);
        }
    }

    private ClusterConfigResource tunableResource(String... permissions) {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        final var validator = new TunableValidator(objectMapper, new ClusterConfigValidatorService(Map.of()));
        final var registry = new RuntimeTunableRegistry(Set.of(LegacyTunable.class, RestrictedLegacyTunable.class),
                CoreRuntimeTunableFeatures.ALL, factory(objectMapper));
        final var tunableService = new RuntimeTunableService(clusterConfigService, validator, new RuntimeTunableSecrets(objectMapper), clusterEventBus, nodeId);
        final Subject subject = mock(Subject.class, withSettings().strictness(Strictness.LENIENT));
        when(subject.isPermitted(anyString())).thenAnswer(invocation -> Set.of(permissions).contains(invocation.<String>getArgument(0)));
        return new ClusterConfigResource(clusterConfigService,
                new RestrictedChainingClassLoader(new ChainingClassLoader(this.getClass().getClassLoader()), SafeClasses.allGraylogInternal()),
                objectMapper,
                clusterConfigValidatorService,
                registry,
                tunableService) {
            @Override
            protected Subject getSubject() {
                return subject;
            }
        };
    }

    private static ByteArrayInputStream json(String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void putRegisteredTunableUsesTunableValidation() {
        final var resource = tunableResource("clusterconfigentry:read", "clusterconfigentry:edit");

        assertThatThrownBy(() -> resource.update(LegacyTunable.class.getName(), json("{\"limit\":0}")))
                .isInstanceOf(ValidationFailureException.class);
        assertThatThrownBy(() -> resource.update(LegacyTunable.class.getName(), json("null")))
                .isInstanceOf(BadRequestException.class);
        verify(clusterConfigService, never()).write(any());
    }

    @Test
    void registeredTunablesRequireTheirOwnPermissions() {
        final var resource = tunableResource("clusterconfigentry:read", "clusterconfigentry:edit", "clusterconfigentry:create");
        final String type = RestrictedLegacyTunable.class.getName();

        assertThatThrownBy(() -> resource.read(type)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> resource.update(type, json("{\"limit\":3}"))).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> resource.delete(type)).isInstanceOf(ForbiddenException.class);
        verify(clusterConfigService, never()).write(any());
        verify(clusterConfigService, never()).remove(any(Class.class));
    }

    @Test
    void deleteRegisteredTunablePostsChangeEvent() {
        when(nodeId.getNodeId()).thenReturn("node-1");
        final var resource = tunableResource("legacy:read", "legacy:edit");

        resource.delete(RestrictedLegacyTunable.class.getName());

        verify(clusterConfigService).remove(RestrictedLegacyTunable.class);
        verify(clusterEventBus).post(any(ClusterConfigChangedEvent.class));
    }

    @Test
    void putClassConsideredUnsafe(@TempDir Path tmpDir) throws IOException {
        final Path file = tmpDir.resolve("secrets.txt");
        Files.writeString(file, "secret content");

        final ClusterConfigResource resource = new ClusterConfigResource(clusterConfigService,
                new RestrictedChainingClassLoader(new ChainingClassLoader(this.getClass().getClassLoader()),
                        SafeClasses.allGraylogInternal()),
                new ObjectMapperProvider().get(),
                clusterConfigValidatorService,
                emptyRegistry(),
                runtimeTunableService
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
                clusterConfigValidatorService,
                emptyRegistry(),
                runtimeTunableService);

        // Simulate how jersey would serialize a File object
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        new FileProvider().writeTo((File) resource.update("java.io.File",
                new ByteArrayInputStream(f("\"%s\"", file.toAbsolutePath()).getBytes(StandardCharsets.UTF_8))).getEntity(), null, null, null, null, null, out);
        final String content = out.toString(StandardCharsets.UTF_8);

        assertThat(content).isEqualTo("secret content");
    }
}
