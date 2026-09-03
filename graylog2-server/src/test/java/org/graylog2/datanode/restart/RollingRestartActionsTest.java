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
package org.graylog2.datanode.restart;

import org.graylog.plugins.datanode.DatanodeClusterAdminAdapter;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.rest.resources.datanodes.DatanodeRestApiProxy;
import org.graylog2.security.AccessToken;
import org.graylog2.security.AccessTokenService;
import org.graylog2.system.processing.control.ClusterProcessingControl;
import org.graylog2.system.processing.control.ClusterProcessingControlFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.threeten.extra.PeriodDuration;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RollingRestartActionsTest {

    @Mock
    DatanodeClusterAdminAdapter clusterAdmin;
    @Mock
    DatanodeRestApiProxy datanodeProxy;
    @Mock
    NodeService<DataNodeDto> nodeService;
    @Mock
    ClusterProcessingControlFactory clusterProcessingControlFactory;
    @Mock
    AccessTokenService accessTokenService;
    @Mock
    ClusterProcessingControl control;
    @Mock
    AccessToken accessToken;

    private RollingRestartActions actions;

    @BeforeEach
    void setUp() {
        actions = new RollingRestartActions(clusterAdmin, datanodeProxy, nodeService,
                clusterProcessingControlFactory, accessTokenService);
    }

    private String expectedBasicHeader(String token) {
        return "Basic " + Base64.getEncoder().encodeToString((token + ":token").getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void pauseProcessing_mintsEphemeralToken_authenticatesFanOut_thenRevokes() {
        when(accessToken.getToken()).thenReturn("s3cr3t");
        when(accessToken.getId()).thenReturn("token-id");
        when(accessTokenService.create(eq("alice"), anyString(), any(PeriodDuration.class))).thenReturn(accessToken);
        when(clusterProcessingControlFactory.create(expectedBasicHeader("s3cr3t"))).thenReturn(control);

        actions.pauseProcessing("alice");

        // Fan-out authenticated with the minted token, presented as HTTP Basic (<token>:token).
        verify(clusterProcessingControlFactory).create(expectedBasicHeader("s3cr3t"));
        verify(control).pauseProcessing();
        verify(control).waitForEmptyBuffers();
        // Token revoked immediately afterwards — no long-lived credential remains.
        verify(accessTokenService).deleteById("token-id");
    }

    @Test
    void resumeProcessing_revokesToken_evenWhenFanOutThrows() {
        when(accessToken.getToken()).thenReturn("s3cr3t");
        when(accessToken.getId()).thenReturn("token-id");
        when(accessTokenService.create(eq("alice"), anyString(), any(PeriodDuration.class))).thenReturn(accessToken);
        when(clusterProcessingControlFactory.create(anyString())).thenReturn(control);
        org.mockito.Mockito.doThrow(new RuntimeException("boom")).when(control).resumeGraylogMessageProcessing();

        assertThatThrownBy(() -> actions.resumeProcessing("alice")).isInstanceOf(RuntimeException.class);

        verify(accessTokenService).deleteById("token-id");
    }
}
