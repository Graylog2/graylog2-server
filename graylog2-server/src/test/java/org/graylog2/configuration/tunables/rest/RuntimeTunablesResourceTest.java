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
package org.graylog2.configuration.tunables.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import org.apache.shiro.subject.Subject;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.configuration.tunables.RuntimeTunable;
import org.graylog2.configuration.tunables.RuntimeTunableDescriptorFactory;
import org.graylog2.configuration.tunables.RuntimeTunableFeature;
import org.graylog2.configuration.tunables.RuntimeTunableRegistry;
import org.graylog2.configuration.tunables.RuntimeTunableSchemaGenerator;
import org.graylog2.configuration.tunables.RuntimeTunableService;
import org.graylog2.configuration.tunables.TunableValidator;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.rest.ValidationFailureException;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.plugin.validate.ClusterConfigValidatorService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RuntimeTunablesResourceTest {
    @RuntimeTunable(id = "test.open", feature = "alpha", title = "Open")
    public record Open(@JsonProperty("limit") @Min(1) int limit) {
        public static Open defaults() {
            return new Open(5);
        }
    }

    @RuntimeTunable(id = "test.restricted", feature = "alpha", title = "Restricted", readPermission = "secret:read", editPermission = "secret:edit")
    public record Restricted(@JsonProperty("enabled") boolean enabled) {
        public static Restricted defaults() {
            return new Restricted(false);
        }
    }

    @Mock
    private ClusterConfigService clusterConfigService;
    @Mock
    private ClusterEventBus clusterEventBus;
    @Mock
    private NodeId nodeId;
    @Mock
    private Subject subject;

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private RuntimeTunablesResource resource;

    @BeforeEach
    void setUp() {
        final var validator = new TunableValidator(objectMapper, new ClusterConfigValidatorService(Map.of()));
        final var registry = new RuntimeTunableRegistry(Set.of(Open.class, Restricted.class),
                Set.of(new RuntimeTunableFeature("alpha", "Alpha", 1), new RuntimeTunableFeature("empty", "Empty", 2)),
                new RuntimeTunableDescriptorFactory(new RuntimeTunableSchemaGenerator(objectMapper), validator, objectMapper));
        final var service = new RuntimeTunableService(clusterConfigService, validator, clusterEventBus, nodeId);
        resource = new RuntimeTunablesResource(registry, service, objectMapper) {
            @Override
            protected Subject getSubject() {
                return subject;
            }
        };
        when(nodeId.getNodeId()).thenReturn("node-1");
        when(subject.isPermitted("clusterconfigentry:read")).thenReturn(true);
        when(subject.isPermitted("clusterconfigentry:edit")).thenReturn(true);
    }

    @Test
    void listsFeaturesWithReadableTunableCounts() {
        final RuntimeTunablesResponse response = resource.list();

        assertThat(response.features()).extracting(RuntimeTunableFeatureSummary::id).containsExactly("alpha", "empty");
        assertThat(response.features()).extracting(RuntimeTunableFeatureSummary::tunableCount).containsExactly(1, 0);
    }

    @Test
    void returnsReadableTunablesOfFeature() {
        final RuntimeTunableFeatureResponse response = resource.getFeature("alpha");

        assertThat(response.id()).isEqualTo("alpha");
        final var tunables = response.tunables();
        assertThat(tunables).extracting(RuntimeTunableResponse::id).containsExactly("test.open");
        assertThat(tunables.get(0).value()).isEqualTo(new Open(5));
        assertThat(tunables.get(0).defaults()).isEqualTo(new Open(5));
        assertThat(tunables.get(0).isDefault()).isTrue();
        assertThat(tunables.get(0).canEdit()).isTrue();
        assertThat(tunables.get(0).schema().path("properties").has("limit")).isTrue();
        assertThat(resource.getFeature("empty").tunables()).isEmpty();
        assertThatThrownBy(() -> resource.getFeature("missing")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void serializesSnakeCaseFlags() throws Exception {
        assertThat(objectMapper.writeValueAsString(resource.list())).contains("\"tunable_count\":1");

        final String json = objectMapper.writeValueAsString(resource.getFeature("alpha"));

        assertThat(json).contains("\"is_default\":true").contains("\"can_edit\":true");
    }

    @Test
    void updatesTunable() {
        final var response = resource.update("test.open", objectMapper.createObjectNode().put("limit", 8));

        assertThat(response.getStatus()).isEqualTo(202);
        verify(clusterConfigService).write(new Open(8));
    }

    @Test
    void rejectsInvalidAndUnparsablePayloads() {
        assertThatThrownBy(() -> resource.update("test.open", objectMapper.createObjectNode().put("limit", 0)))
                .isInstanceOf(ValidationFailureException.class);
        assertThatThrownBy(() -> resource.update("test.open", objectMapper.createObjectNode().put("limit", "many")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void enforcesPermissionsAndUnknownIds() {
        assertThatThrownBy(() -> resource.update("test.restricted", objectMapper.createObjectNode().put("enabled", true)))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> resource.reset("test.restricted")).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> resource.update("missing", objectMapper.createObjectNode())).isInstanceOf(NotFoundException.class);
    }

    @Test
    void resetsTunable() {
        final var response = resource.reset("test.open");

        verify(clusterConfigService).remove(Open.class);
        assertThat(response.isDefault()).isTrue();
    }

    @Test
    void doesNotAuditRequestEntityOfUpdates() throws Exception {
        final var update = RuntimeTunablesResource.class.getMethod("update", String.class, JsonNode.class);

        assertThat(update.isAnnotationPresent(PUT.class)).isTrue();
        assertThat(update.getAnnotation(AuditEvent.class).captureRequestEntity()).isFalse();
    }

    @Test
    void rejectsNullBody() {
        assertThatThrownBy(() -> resource.update("test.open", objectMapper.nullNode())).isInstanceOf(BadRequestException.class);
    }
}
