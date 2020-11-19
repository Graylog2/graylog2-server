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
package org.graylog2.audit;

import org.graylog2.plugin.system.NodeId;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuditActorTest {
    @Test
    public void testUser() throws Exception {
        final AuditActor actor = AuditActor.user("jane");

        assertThat(actor.urn()).isEqualTo("urn:graylog:user:jane");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyUser() throws Exception {
        AuditActor.user("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullUser() throws Exception {
        AuditActor.user(null);
    }

    @Test
    public void testSystem() throws Exception {
        final NodeId nodeId = mock(NodeId.class);
        when(nodeId.toString()).thenReturn("28164cbe-4ad9-4c9c-a76e-088655aa78892");
        final AuditActor actor = AuditActor.system(nodeId);

        assertThat(actor.urn()).isEqualTo("urn:graylog:node:28164cbe-4ad9-4c9c-a76e-088655aa78892");
    }

    @Test(expected = NullPointerException.class)
    public void testNullSystem() throws Exception {
        AuditActor.system(null);
    }
}