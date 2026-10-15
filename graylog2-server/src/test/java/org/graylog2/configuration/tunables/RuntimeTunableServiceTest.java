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
package org.graylog2.configuration.tunables;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.Min;
import org.graylog2.cluster.ClusterConfigChangedEvent;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.rest.ValidationFailureException;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.plugin.validate.ClusterConfigValidatorService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuntimeTunableServiceTest {
    @RuntimeTunable(id = "test.limit", feature = "search", title = "Limit")
    public record Limit(@JsonProperty("limit") @Min(1) int limit) {
        public static Limit defaults() {
            return new Limit(5);
        }
    }

    @Mock
    private ClusterConfigService clusterConfigService;
    @Mock
    private ClusterEventBus clusterEventBus;
    @Mock
    private NodeId nodeId;

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private RuntimeTunableService service;
    private RuntimeTunableDescriptorFactory factory;

    @BeforeEach
    void setUp() {
        final var validator = new TunableValidator(objectMapper, new ClusterConfigValidatorService(Map.of()));
        factory = new RuntimeTunableDescriptorFactory(new RuntimeTunableSchemaGenerator(objectMapper), validator, objectMapper);
        service = new RuntimeTunableService(clusterConfigService, validator, clusterEventBus, nodeId);
    }

    private RuntimeTunableDescriptor descriptor(Class<?> type) throws Exception {
        return factory.create(type, Set.of("search"));
    }

    @Test
    void readsDefaultsWhenNothingStored() throws Exception {
        assertThat(service.read(descriptor(Limit.class))).isEqualTo(new RuntimeTunableService.Value(new Limit(5), true));
    }

    @Test
    void readsStoredValue() throws Exception {
        when(clusterConfigService.get(Limit.class)).thenReturn(new Limit(9));

        assertThat(service.read(descriptor(Limit.class))).isEqualTo(new RuntimeTunableService.Value(new Limit(9), false));
    }

    @Test
    void writesValidConfigAndRejectsInvalidConfig() throws Exception {
        final var limit = descriptor(Limit.class);
        service.update(limit, new Limit(7));
        verify(clusterConfigService).write(new Limit(7));

        assertThatThrownBy(() -> service.update(limit, new Limit(0)))
                .isInstanceOfSatisfying(ValidationFailureException.class,
                        e -> assertThat(e.getValidationResult().getErrors()).containsOnlyKeys("limit"));
    }

    @Test
    void resetRemovesAndPostsChangeEvent() throws Exception {
        when(nodeId.getNodeId()).thenReturn("node-1");

        service.reset(descriptor(Limit.class));

        verify(clusterConfigService).remove(Limit.class);
        verify(clusterConfigService, never()).write(any());
        final var event = ArgumentCaptor.forClass(ClusterConfigChangedEvent.class);
        verify(clusterEventBus).post(event.capture());
        assertThat(event.getValue().type()).isEqualTo(Limit.class.getCanonicalName());
        assertThat(event.getValue().nodeId()).isEqualTo("node-1");
    }
}
