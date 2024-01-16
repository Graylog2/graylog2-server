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

import org.graylog.security.certutil.CertRenewalService;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.datanode.DataNodeService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import jakarta.ws.rs.NotFoundException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class DataNodeManagementResourceTest {

    public static final String NODEID = "nodeid";
    private DataNodeManagementResource classUnderTest;

    @Mock
    private DataNodeService dataNodeService;
    @Mock
    private NodeService<DataNodeDto> nodeService;
    @Mock
    private CertRenewalService certRenewalService;

    @Before
    public void setUp() {
        classUnderTest = new DataNodeManagementResource(dataNodeService, nodeService, certRenewalService);
    }

    @Test
    public void removeUnavailableNode_throwsNotFoundException() throws NodeNotFoundException {
        doThrow(NodeNotFoundException.class).when(dataNodeService).removeNode(NODEID);
        Exception e = assertThrows(NotFoundException.class, () -> classUnderTest.removeNode(NODEID));
        assertEquals("Node " + NODEID + " not found", e.getMessage());
    }

    @Test
    public void resetUnavailableNode_throwsNotFoundException() throws NodeNotFoundException {
        doThrow(NodeNotFoundException.class).when(dataNodeService).resetNode(NODEID);
        Exception e = assertThrows(NotFoundException.class, () -> classUnderTest.resetNode(NODEID));
        assertEquals("Node " + NODEID + " not found", e.getMessage());
    }

    @Test
    public void verifyRemoveServiceCalled() throws NodeNotFoundException {
        classUnderTest.removeNode(NODEID);
        verify(dataNodeService).removeNode(NODEID);
    }

    @Test
    public void verifyResetServiceCalled() throws NodeNotFoundException {
        classUnderTest.resetNode(NODEID);
        verify(dataNodeService).resetNode(NODEID);
    }


}
