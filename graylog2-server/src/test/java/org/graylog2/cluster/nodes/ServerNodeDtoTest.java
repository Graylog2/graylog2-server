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

import org.graylog2.plugin.lifecycles.Lifecycle;
import org.graylog2.plugin.lifecycles.LoadBalancerStatus;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;

class ServerNodeDtoTest {

    @Test
    void testGetLoadBalancerStatus() {
        assertThat(dto(b -> b.setLifecycle(Lifecycle.RUNNING)).getLoadBalancerStatus()).isEqualTo(LoadBalancerStatus.ALIVE);
        assertThat(dto(b -> b.setLifecycle(Lifecycle.HALTING)).getLoadBalancerStatus()).isEqualTo(LoadBalancerStatus.DEAD);
        assertThat(dto(b -> b.setLifecycle(Lifecycle.THROTTLED)).getLoadBalancerStatus()).isEqualTo(LoadBalancerStatus.THROTTLED);
        assertThat(dto(b -> b.setLifecycle(null)).getLoadBalancerStatus()).isNull();
    }

    @Test
    void testToEntityParameters() {
        final Map<String, Object> params = dto(b -> b
                .setProcessing(true)
                .setLifecycle(Lifecycle.RUNNING)
                .setVersion("6.2.0")
        ).toEntityParameters();

        assertThat(params)
                .containsEntry("node_id", "test-node-id")
                .containsEntry("is_leader", false)
                .containsEntry(ServerNodeDto.FIELD_IS_PROCESSING, true)
                .containsEntry(ServerNodeDto.FIELD_LIFECYCLE, Lifecycle.RUNNING.name())
                .containsEntry(ServerNodeDto.FIELD_VERSION, "6.2.0");
    }

    @Test
    void testToEntityParametersOmitsNullOptionalFields() {
        final Map<String, Object> params = dto(b -> b).toEntityParameters();

        assertThat(params).doesNotContainKeys(ServerNodeDto.FIELD_LIFECYCLE, ServerNodeDto.FIELD_VERSION);
    }

    private ServerNodeDto dto(UnaryOperator<ServerNodeDto.Builder> customizer) {
        final ServerNodeDto.Builder base = ServerNodeDto.Builder.builder()
                .setId("test-node-id")
                .setLeader(false);
        return customizer.apply(base).build();
    }
}
