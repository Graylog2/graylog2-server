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
package org.graylog.plugins.sidecar.opamp;

import com.google.protobuf.ByteString;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import opamp.proto.Anyvalue.AnyValue;
import opamp.proto.Anyvalue.KeyValue;
import opamp.proto.Opamp.AgentDescription;
import opamp.proto.Opamp.AgentToServer;
import opamp.proto.Opamp.RemoteConfigStatus;
import opamp.proto.Opamp.RemoteConfigStatuses;
import opamp.proto.Opamp.ServerToAgent;
import org.graylog.plugins.sidecar.opamp.config.ConfigurationGenerator;
import org.graylog.plugins.sidecar.opamp.server.OpAMPFrameHandler;
import org.graylog.plugins.sidecar.rest.models.NodeDetails;
import org.graylog.plugins.sidecar.rest.models.Sidecar;
import org.graylog.plugins.sidecar.services.ActionService;
import org.graylog.plugins.sidecar.services.SidecarRegistrationService;
import org.graylog.plugins.sidecar.services.SidecarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OpAMPFrameHandlerTest {

    @Mock
    private SidecarService sidecarService;

    @Mock
    private ActionService actionService;

    @Mock
    private ConfigurationGenerator configurationGenerator;

    private SidecarRegistrationService registrationService;
    private OpAMPFrameHandler handler;
    private EmbeddedChannel channel;

    @BeforeEach
    void setUp() {
        // Create real registration service with mocked dependencies
        registrationService = new SidecarRegistrationService(sidecarService, actionService);
        handler = new OpAMPFrameHandler(registrationService, configurationGenerator);
        channel = new EmbeddedChannel(handler) {
            @Override
            protected SocketAddress remoteAddress0() {
                return new InetSocketAddress("192.168.1.100", 12345);
            }
        };
    }

    @Test
    void testAgentRegistration() {
        // Given: A new agent connecting with agent_description
        UUID agentUuid = UUID.randomUUID();
        byte[] uuidBytes = uuidToBytes(agentUuid);

        AgentToServer agentMessage = AgentToServer.newBuilder()
                .setInstanceUid(ByteString.copyFrom(uuidBytes))
                .setSequenceNum(1)
                .setAgentDescription(AgentDescription.newBuilder()
                        .addIdentifyingAttributes(keyValue("service.name", "test-sidecar"))
                        .addIdentifyingAttributes(keyValue("service.version", "2.0.0"))
                        .addNonIdentifyingAttributes(keyValue("os.type", "linux"))
                        .addNonIdentifyingAttributes(keyValue("host.name", "test-host"))
                        .addNonIdentifyingAttributes(keyValue("host.ip", "192.168.1.50"))
                        .build())
                .build();

        // Mock the tag assignment update to return the sidecar unchanged
        when(sidecarService.updateTaggedConfigurationAssignments(any())).thenAnswer(inv -> inv.getArgument(0));

        // Mock the config generator to return empty config (no assignments)
        when(configurationGenerator.computeConfigHash(any())).thenReturn(new byte[32]);
        when(configurationGenerator.generateConfig(any())).thenReturn("extensions:\n  sidecar:\n    collectors: []\n");

        // When: Agent sends registration message
        channel.writeInbound(frameRequest(agentMessage));

        // Then: Sidecar should be saved
        ArgumentCaptor<Sidecar> sidecarCaptor = ArgumentCaptor.forClass(Sidecar.class);
        verify(sidecarService).save(sidecarCaptor.capture());

        Sidecar savedSidecar = sidecarCaptor.getValue();
        assertThat(savedSidecar.nodeId()).isEqualTo(agentUuid.toString());
        assertThat(savedSidecar.nodeName()).isEqualTo("test-sidecar");
        assertThat(savedSidecar.sidecarVersion()).isEqualTo("2.0.0");
        assertThat(savedSidecar.nodeDetails().operatingSystem()).isEqualTo("linux");
        assertThat(savedSidecar.nodeDetails().ip()).isEqualTo("192.168.1.50");

        // And: Response should be sent back
        BinaryWebSocketFrame responseFrame = channel.readOutbound();
        assertThat(responseFrame).isNotNull();

        ServerToAgent response = parseResponse(responseFrame);
        assertThat(response.getInstanceUid()).isEqualTo(ByteString.copyFrom(uuidBytes));

        // Config should be included since agent has no config hash yet
        assertThat(response.hasRemoteConfig()).isTrue();
    }

    @Test
    void testConfigDeliveryOnHashMismatch() {
        // Given: An existing sidecar with assignments
        UUID agentUuid = UUID.randomUUID();
        byte[] uuidBytes = uuidToBytes(agentUuid);

        Sidecar existingSidecar = Sidecar.builder()
                .id("sidecar-id")
                .nodeId(agentUuid.toString())
                .nodeName("existing-sidecar")
                .nodeDetails(NodeDetails.create("linux", "192.168.1.50", null, null, null, Set.of(), "/etc/graylog"))
                .sidecarVersion("2.0.0")
                .lastSeen(org.joda.time.DateTime.now())
                .assignments(List.of())
                .build();

        when(sidecarService.findByNodeId(agentUuid.toString())).thenReturn(existingSidecar);
        when(sidecarService.updateTaggedConfigurationAssignments(any())).thenAnswer(inv -> inv.getArgument(0));

        // Server has a different config hash than agent
        byte[] serverConfigHash = "server-config-hash-12345678".getBytes();
        byte[] agentConfigHash = "agent-config-hash-87654321".getBytes();

        when(configurationGenerator.computeConfigHash(any())).thenReturn(serverConfigHash);
        when(configurationGenerator.generateConfig(any())).thenReturn("extensions:\n  sidecar:\n    collectors:\n      - id: config1\n");

        // Agent sends message with its current config hash
        AgentToServer agentMessage = AgentToServer.newBuilder()
                .setInstanceUid(ByteString.copyFrom(uuidBytes))
                .setSequenceNum(2)
                .setRemoteConfigStatus(RemoteConfigStatus.newBuilder()
                        .setLastRemoteConfigHash(ByteString.copyFrom(agentConfigHash))
                        .setStatus(RemoteConfigStatuses.RemoteConfigStatuses_APPLIED)
                        .build())
                .build();

        // When: Agent sends heartbeat with outdated config hash
        channel.writeInbound(frameRequest(agentMessage));

        // Then: Response should include new config
        BinaryWebSocketFrame responseFrame = channel.readOutbound();
        ServerToAgent response = parseResponse(responseFrame);

        assertThat(response.hasRemoteConfig()).isTrue();
        assertThat(response.getRemoteConfig().getConfigHash())
                .isEqualTo(ByteString.copyFrom(serverConfigHash));
        assertThat(response.getRemoteConfig().getConfig().getConfigMapMap())
                .containsKey("sidecar.yaml");
    }

    @Test
    void testNoConfigDeliveryOnHashMatch() {
        // Given: An existing sidecar
        UUID agentUuid = UUID.randomUUID();
        byte[] uuidBytes = uuidToBytes(agentUuid);

        Sidecar existingSidecar = Sidecar.builder()
                .id("sidecar-id")
                .nodeId(agentUuid.toString())
                .nodeName("existing-sidecar")
                .nodeDetails(NodeDetails.create("linux", "192.168.1.50", null, null, null, Set.of(), "/etc/graylog"))
                .sidecarVersion("2.0.0")
                .lastSeen(org.joda.time.DateTime.now())
                .assignments(List.of())
                .build();

        when(sidecarService.findByNodeId(agentUuid.toString())).thenReturn(existingSidecar);
        when(sidecarService.updateTaggedConfigurationAssignments(any())).thenAnswer(inv -> inv.getArgument(0));

        // Server and agent have the same config hash
        byte[] configHash = "same-config-hash-123456789".getBytes();

        when(configurationGenerator.computeConfigHash(any())).thenReturn(configHash);

        // Agent sends message with matching config hash
        AgentToServer agentMessage = AgentToServer.newBuilder()
                .setInstanceUid(ByteString.copyFrom(uuidBytes))
                .setSequenceNum(3)
                .setRemoteConfigStatus(RemoteConfigStatus.newBuilder()
                        .setLastRemoteConfigHash(ByteString.copyFrom(configHash))
                        .setStatus(RemoteConfigStatuses.RemoteConfigStatuses_APPLIED)
                        .build())
                .build();

        // When: Agent sends heartbeat with matching config hash
        channel.writeInbound(frameRequest(agentMessage));

        // Then: Response should NOT include config (hashes match)
        BinaryWebSocketFrame responseFrame = channel.readOutbound();
        ServerToAgent response = parseResponse(responseFrame);

        assertThat(response.hasRemoteConfig()).isFalse();
    }

    @Test
    void testHostNameFallbackForNodeName() {
        // Given: Agent without service.name but with host.name
        UUID agentUuid = UUID.randomUUID();
        byte[] uuidBytes = uuidToBytes(agentUuid);

        AgentToServer agentMessage = AgentToServer.newBuilder()
                .setInstanceUid(ByteString.copyFrom(uuidBytes))
                .setSequenceNum(1)
                .setAgentDescription(AgentDescription.newBuilder()
                        .addIdentifyingAttributes(keyValue("service.version", "1.0.0"))
                        // No service.name - should fallback to host.name
                        .addNonIdentifyingAttributes(keyValue("host.name", "fallback-hostname"))
                        .addNonIdentifyingAttributes(keyValue("os.type", "windows"))
                        .build())
                .build();

        when(sidecarService.updateTaggedConfigurationAssignments(any())).thenAnswer(inv -> inv.getArgument(0));
        when(configurationGenerator.computeConfigHash(any())).thenReturn(new byte[32]);
        when(configurationGenerator.generateConfig(any())).thenReturn("");

        // When
        channel.writeInbound(frameRequest(agentMessage));

        // Then: nodeName should be the host.name
        ArgumentCaptor<Sidecar> sidecarCaptor = ArgumentCaptor.forClass(Sidecar.class);
        verify(sidecarService).save(sidecarCaptor.capture());

        assertThat(sidecarCaptor.getValue().nodeName()).isEqualTo("fallback-hostname");
    }

    // Helper methods

    private byte[] uuidToBytes(UUID uuid) {
        ByteBuffer bb = ByteBuffer.allocate(16);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    private KeyValue keyValue(String key, String value) {
        return KeyValue.newBuilder()
                .setKey(key)
                .setValue(AnyValue.newBuilder().setStringValue(value).build())
                .build();
    }

    /**
     * Wraps a protobuf message with OpAMP WebSocket framing (varint 0 header).
     */
    private BinaryWebSocketFrame frameRequest(AgentToServer message) {
        byte[] protoBytes = message.toByteArray();
        byte[] framedBytes = new byte[1 + protoBytes.length];
        framedBytes[0] = 0;  // varint-encoded 0 header
        System.arraycopy(protoBytes, 0, framedBytes, 1, protoBytes.length);
        return new BinaryWebSocketFrame(Unpooled.wrappedBuffer(framedBytes));
    }

    /**
     * Parses a ServerToAgent response, skipping the OpAMP WebSocket framing header.
     */
    private ServerToAgent parseResponse(BinaryWebSocketFrame frame) {
        try {
            byte[] bytes = new byte[frame.content().readableBytes()];
            frame.content().readBytes(bytes);
            // Skip the 1-byte varint header (value 0)
            if (bytes.length > 0 && bytes[0] == 0) {
                bytes = java.util.Arrays.copyOfRange(bytes, 1, bytes.length);
            }
            return ServerToAgent.parseFrom(bytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse ServerToAgent response", e);
        }
    }
}
