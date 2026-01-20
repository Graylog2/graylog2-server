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
package org.graylog.plugins.sidecar.opamp.server;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import opamp.proto.Anyvalue.KeyValue;
import opamp.proto.Opamp.AgentConfigFile;
import opamp.proto.Opamp.AgentConfigMap;
import opamp.proto.Opamp.AgentDescription;
import opamp.proto.Opamp.AgentRemoteConfig;
import opamp.proto.Opamp.AgentToServer;
import opamp.proto.Opamp.RemoteConfigStatuses;
import opamp.proto.Opamp.ServerToAgent;
import org.graylog.plugins.sidecar.opamp.config.ConfigurationGenerator;
import org.graylog.plugins.sidecar.rest.models.AgentState;
import org.graylog.plugins.sidecar.rest.models.NodeDetails;
import org.graylog.plugins.sidecar.rest.models.ServerDirectives;
import org.graylog.plugins.sidecar.rest.models.Sidecar;
import org.graylog.plugins.sidecar.services.SidecarRegistrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Singleton
@ChannelHandler.Sharable
public class OpAMPFrameHandler extends SimpleChannelInboundHandler<BinaryWebSocketFrame> {
    private static final Logger LOG = LoggerFactory.getLogger(OpAMPFrameHandler.class);

    private final SidecarRegistrationService registrationService;
    private final ConfigurationGenerator configurationGenerator;

    @Inject
    public OpAMPFrameHandler(SidecarRegistrationService registrationService,
                            ConfigurationGenerator configurationGenerator) {
        this.registrationService = registrationService;
        this.configurationGenerator = configurationGenerator;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, BinaryWebSocketFrame frame) {
        final ByteBuf content = frame.content();
        final byte[] bytes = new byte[content.readableBytes()];
        content.readBytes(bytes);

        try {
            // OpAMP WebSocket messages have a varint header (currently 0)
            // Check if header is present and skip it
            int offset = 0;
            if (bytes.length > 0 && bytes[0] == 0) {
                // Decode varint to find header length and skip it
                final CodedInputStream cis = CodedInputStream.newInstance(bytes);
                final long header = cis.readUInt64();
                if (header != 0) {
                    LOG.warn("Unexpected non-zero OpAMP message header: {}", header);
                }
                offset = cis.getTotalBytesRead();
            }

            // Parse the protobuf message after the header
            final byte[] protoBytes = offset > 0 ?
                    java.util.Arrays.copyOfRange(bytes, offset, bytes.length) : bytes;

            AgentToServer message;
            try {
                message = AgentToServer.parseFrom(protoBytes);
            } catch (InvalidProtocolBufferException e) {
                // If parsing failed and we didn't skip a header, try skipping 1 byte
                // (in case the header check failed for some reason)
                if (offset == 0 && bytes.length > 1) {
                    LOG.debug("Retrying parse after skipping first byte (length={}, first bytes={},{},{})",
                            bytes.length,
                            bytes.length > 0 ? String.format("0x%02X", bytes[0]) : "N/A",
                            bytes.length > 1 ? String.format("0x%02X", bytes[1]) : "N/A",
                            bytes.length > 2 ? String.format("0x%02X", bytes[2]) : "N/A");
                    message = AgentToServer.parseFrom(java.util.Arrays.copyOfRange(bytes, 1, bytes.length));
                } else {
                    LOG.error("Failed to parse message (length={}, offset={}, first bytes={},{},{})",
                            bytes.length, offset,
                            bytes.length > 0 ? String.format("0x%02X", bytes[0]) : "N/A",
                            bytes.length > 1 ? String.format("0x%02X", bytes[1]) : "N/A",
                            bytes.length > 2 ? String.format("0x%02X", bytes[2]) : "N/A");
                    throw e;
                }
            }
            LOG.debug("Received AgentToServer message from {}: {}", ctx.channel().remoteAddress(), message);

            // Process the message and build response
            final ServerToAgent response = processMessage(message);

            // Send response with header prefix
            final byte[] responseBytes = response.toByteArray();
            final byte[] framedResponse = new byte[1 + responseBytes.length];
            framedResponse[0] = 0;  // varint-encoded 0 header
            System.arraycopy(responseBytes, 0, framedResponse, 1, responseBytes.length);

            ctx.writeAndFlush(new BinaryWebSocketFrame(
                    Unpooled.wrappedBuffer(framedResponse)));

        } catch (InvalidProtocolBufferException e) {
            LOG.error("Failed to parse AgentToServer message (bytes.length={}, first bytes: {},{},{})",
                    bytes.length,
                    bytes.length > 0 ? String.format("0x%02X", bytes[0]) : "N/A",
                    bytes.length > 1 ? String.format("0x%02X", bytes[1]) : "N/A",
                    bytes.length > 2 ? String.format("0x%02X", bytes[2]) : "N/A",
                    e);
            ctx.close();
        } catch (Exception e) {
            LOG.error("Error processing AgentToServer message", e);
            ctx.close();
        }
    }

    private ServerToAgent processMessage(AgentToServer message) {
        final String nodeId = extractNodeId(message.getInstanceUid());
        final AgentState agentState = mapToAgentState(nodeId, message);
        final ServerDirectives directives = registrationService.checkIn(agentState);
        final Sidecar sidecar = directives.sidecar();

        LOG.info("Processed check-in for OpAMP agent: nodeId={}, nodeName={}", nodeId, sidecar.nodeName());

        final ServerToAgent.Builder responseBuilder = ServerToAgent.newBuilder()
                .setInstanceUid(message.getInstanceUid());

        if (message.hasRemoteConfigStatus()) {
            logRemoteConfigStatus(nodeId, message);
        }

        final byte[] agentConfigHash = getAgentConfigHash(message);
        final ConfigurationGenerator.GeneratedConfig generatedConfig = configurationGenerator.generateConfigWithHash(sidecar);
        if (!Arrays.equals(generatedConfig.hash(), agentConfigHash)) {
            responseBuilder.setRemoteConfig(buildRemoteConfig(generatedConfig));
            LOG.info("Sending remote config to agent: nodeId={}", nodeId);
        }

        // TODO: deliver actions via custom message when needed
        if (!directives.actions().isEmpty()) {
            LOG.debug("Pending actions for agent (not yet delivered via OpAMP): nodeId={}, actions={}",
                    nodeId, directives.actions().size());
        }

        return responseBuilder.build();
    }

    /**
     * Maps an OpAMP AgentToServer message to the transport-agnostic AgentState.
     */
    private AgentState mapToAgentState(String nodeId, AgentToServer message) {
        String nodeName = nodeId;
        String sidecarVersion = "unknown";
        NodeDetails nodeDetails = NodeDetails.create("unknown", null, null, null, null, Set.of(), null);

        if (message.hasAgentDescription()) {
            final AgentDescription desc = message.getAgentDescription();
            final ParsedAgentDescription parsed = parseAgentDescription(desc);

            nodeName = parsed.nodeName != null ? parsed.nodeName : nodeId;
            sidecarVersion = parsed.version;
            nodeDetails = NodeDetails.create(
                    parsed.operatingSystem,
                    parsed.ip,
                    null,
                    null,
                    null,
                    parsed.tags,
                    parsed.collectorConfigurationDirectory
            );
        }

        return AgentState.create(nodeId, nodeName, sidecarVersion, nodeDetails);
    }

    /**
     * Parsed values from AgentDescription.
     */
    private record ParsedAgentDescription(
            String nodeName,
            String version,
            String operatingSystem,
            String ip,
            String collectorConfigurationDirectory,
            Set<String> tags
    ) {}

    /**
     * Parses identifying and non-identifying attributes from AgentDescription.
     */
    private ParsedAgentDescription parseAgentDescription(AgentDescription agentDescription) {
        String nodeName = null;
        String operatingSystem = "unknown";
        String ip = null;
        String version = "unknown";
        String collectorConfigurationDirectory = null;
        Set<String> tags = Set.of();

        // Process identifying attributes (service.name, service.version, etc.)
        for (KeyValue kv : agentDescription.getIdentifyingAttributesList()) {
            final String key = kv.getKey();
            final String value = getStringValue(kv);
            switch (key) {
                case "service.name" -> nodeName = value;
                case "service.version" -> version = value;
            }
        }

        // Process non-identifying attributes (os.type, host.name, host.ip, sidecar.*, etc.)
        for (KeyValue kv : agentDescription.getNonIdentifyingAttributesList()) {
            final String key = kv.getKey();
            final String value = getStringValue(kv);
            switch (key) {
                case "os.type" -> operatingSystem = value;
                case "host.ip" -> ip = value;
                case "host.name" -> {
                    // Use host.name as fallback for nodeName if service.name not set
                    if (nodeName == null) {
                        nodeName = value;
                    }
                }
                case "sidecar.config_directory" -> collectorConfigurationDirectory = value;
                case "sidecar.tags" -> {
                    if (value != null && !value.isEmpty()) {
                        tags = Set.of(value.split(","));
                    }
                }
            }
        }

        return new ParsedAgentDescription(nodeName, version, operatingSystem, ip, collectorConfigurationDirectory, tags);
    }

    private void logRemoteConfigStatus(String nodeId, AgentToServer message) {
        final var configStatus = message.getRemoteConfigStatus();
        final RemoteConfigStatuses status = configStatus.getStatus();

        if (status == RemoteConfigStatuses.RemoteConfigStatuses_APPLIED) {
            LOG.debug("Agent applied config successfully: nodeId={}", nodeId);
        } else if (status == RemoteConfigStatuses.RemoteConfigStatuses_APPLYING) {
            LOG.debug("Agent is applying config: nodeId={}", nodeId);
        } else if (status == RemoteConfigStatuses.RemoteConfigStatuses_FAILED) {
            LOG.warn("Agent failed to apply config: nodeId={}, error={}",
                    nodeId, configStatus.getErrorMessage());
        } else {
            LOG.debug("Agent config status: nodeId={}, status={}", nodeId, status);
        }
    }

    private byte[] getAgentConfigHash(AgentToServer message) {
        if (message.hasRemoteConfigStatus()) {
            final byte[] hash = message.getRemoteConfigStatus().getLastRemoteConfigHash().toByteArray();
            if (hash.length > 0) {
                return hash;
            }
        }
        return null;
    }

    private AgentRemoteConfig buildRemoteConfig(ConfigurationGenerator.GeneratedConfig generatedConfig) {
        final AgentConfigFile configFile = AgentConfigFile.newBuilder()
                .setBody(ByteString.copyFromUtf8(generatedConfig.yaml()))
                .setContentType("application/x-yaml")
                .build();

        final AgentConfigMap configMap = AgentConfigMap.newBuilder()
                .putConfigMap("sidecar.yaml", configFile)
                .build();

        return AgentRemoteConfig.newBuilder()
                .setConfig(configMap)
                .setConfigHash(ByteString.copyFrom(generatedConfig.hash()))
                .build();
    }

    private String getStringValue(KeyValue kv) {
        if (kv.hasValue() && kv.getValue().hasStringValue()) {
            return kv.getValue().getStringValue();
        }
        return "";
    }

    private String extractNodeId(ByteString instanceUid) {
        // Convert the 16-byte UUID to a UUID string for use as nodeId
        if (instanceUid.isEmpty() || instanceUid.size() != 16) {
            LOG.warn("Invalid instance_uid size: {}, expected 16 bytes", instanceUid.size());
            return "unknown-" + System.currentTimeMillis();
        }

        // Convert bytes to UUID string format (e.g., "019bb696-f3eb-73bd-8ed4-c79c8103e627")
        final ByteBuffer bb = ByteBuffer.wrap(instanceUid.toByteArray());
        return new UUID(bb.getLong(), bb.getLong()).toString();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        LOG.debug("OpAMP connection established from {}", ctx.channel().remoteAddress());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LOG.debug("OpAMP connection closed from {}", ctx.channel().remoteAddress());
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOG.error("Error in OpAMP handler", cause);
        ctx.close();
    }
}
