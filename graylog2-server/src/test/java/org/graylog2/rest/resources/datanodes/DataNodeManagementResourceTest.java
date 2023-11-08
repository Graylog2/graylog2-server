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
package org.graylog2.rest.resources.datanodes;

import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.datanode.DataNodeService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.ws.rs.NotFoundException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@RunWith(MockitoJUnitRunner.class)
public class DataNodeManagementResourceTest {

    private DataNodeManagementResource classUnderTest;

    @Mock
    private DataNodeService dataNodeService;

    @Before
    public void setUp() {
        classUnderTest = new DataNodeManagementResource(dataNodeService);
    }

    @Test
    public void removeUnavailableNode_throwsNotFoundException() throws NodeNotFoundException {
        doThrow(NodeNotFoundException.class).when(dataNodeService).removeNode(any());
        Exception e = assertThrows(NotFoundException.class, () -> classUnderTest.removeNode("nodeid"));
        assertEquals("Node nodeid not found", e.getMessage());
    }


}
