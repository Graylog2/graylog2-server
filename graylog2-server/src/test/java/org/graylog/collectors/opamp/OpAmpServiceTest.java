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
package org.graylog.collectors.opamp;

import com.google.protobuf.ByteString;
import opamp.proto.Anyvalue;
import org.graylog.collectors.CollectorCaService;
import org.graylog.collectors.CollectorInstanceService;
import org.graylog.collectors.CollectorsConfigService;
import org.graylog.collectors.FleetTransactionLogService;
import org.graylog.collectors.SourceService;
import org.graylog.collectors.db.Attribute;
import org.graylog.collectors.db.EnrollmentTokenCreator;
import org.graylog.collectors.db.EnrollmentTokenDTO;
import org.graylog.collectors.opamp.auth.AgentTokenService;
import org.graylog.collectors.opamp.auth.EnrollmentTokenService;
import org.graylog.collectors.opamp.transport.OpAmpAuthContext;
import org.graylog.security.pki.CertificateService;
import org.graylog2.plugin.cluster.ClusterIdService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.shared.utilities.StringUtils.f;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link OpAmpService} authentication dispatch logic and Collector-submitted
 * attribute sanitization ({@code extractAttributes}).
 */
@ExtendWith(MockitoExtension.class)
class OpAmpServiceTest {

    @Mock
    private EnrollmentTokenService enrollmentTokenService;

    @Mock
    private AgentTokenService agentTokenService;

    @Mock
    private CollectorCaService collectorCaService;

    @Mock
    private CertificateService certificateService;

    @Mock
    private CollectorInstanceService collectorInstanceService;

    @Mock
    private CollectorsConfigService collectorsConfigService;

    @Mock
    private ClusterIdService clusterIdService;

    @Mock
    private FleetTransactionLogService fleetTransactionLogService;

    @Mock
    private SourceService sourceService;

    private OpAmpService opAmpService;

    private static final OpAmpAuthContext.Transport TRANSPORT = OpAmpAuthContext.Transport.HTTP;

    @BeforeEach
    void setUp() {
        lenient().when(clusterIdService.getString()).thenReturn("clusterId");
        opAmpService = new OpAmpService(enrollmentTokenService, agentTokenService, collectorCaService, certificateService,
                collectorInstanceService, collectorsConfigService, clusterIdService, fleetTransactionLogService, sourceService);
    }

    @Test
    void authenticateDispatchesEnrollmentTokenByCttHeader() {
        // Create a token with ctt: enrollment
        final String token = createTokenWithCtt("enrollment");
        final String authHeader = "Bearer " + token;
        final EnrollmentTokenDTO tokenDto = new EnrollmentTokenDTO("token-id", "test-token", "jti-1", "kid-1", "test-fleet",
                new EnrollmentTokenCreator("user-id", "admin"), Instant.now(), null, 0, null);

        when(enrollmentTokenService.validateToken(eq(token)))
                .thenReturn(Optional.of(tokenDto));

        final Optional<OpAmpAuthContext> result = opAmpService.authenticate(authHeader, TRANSPORT);

        assertThat(result).isPresent();
        assertThat(result.get()).isInstanceOf(OpAmpAuthContext.Enrollment.class);
        verify(enrollmentTokenService).validateToken(token);
        verify(agentTokenService, never()).validateAgentToken(any(), any());
    }

    @Test
    void authenticateDispatchesAgentTokenByCttHeader() {
        // Create a token with ctt: agent
        final String token = createTokenWithCtt("agent");
        final String authHeader = "Bearer " + token;
        final String instanceUid = "instance-uid";
        final OpAmpAuthContext.Identified expectedContext = new OpAmpAuthContext.Identified(instanceUid, TRANSPORT);

        when(agentTokenService.validateAgentToken(eq(token), eq(TRANSPORT)))
                .thenReturn(Optional.of(expectedContext));

        final Optional<OpAmpAuthContext> result = opAmpService.authenticate(authHeader, TRANSPORT);

        assertThat(result).isPresent();
        assertThat(result.get()).isInstanceOf(OpAmpAuthContext.Identified.class);
        verify(agentTokenService).validateAgentToken(token, TRANSPORT);
        verify(enrollmentTokenService, never()).validateToken(any());
    }

    @Test
    void authenticateReturnsEmptyForUnknownCttHeader() {
        // Create a token with unknown ctt
        final String token = createTokenWithCtt("unknown");
        final String authHeader = "Bearer " + token;

        final Optional<OpAmpAuthContext> result = opAmpService.authenticate(authHeader, TRANSPORT);

        assertThat(result).isEmpty();
        verify(enrollmentTokenService, never()).validateToken(any());
        verify(agentTokenService, never()).validateAgentToken(any(), any());
    }

    @Test
    void authenticateReturnsEmptyForMissingCttHeader() {
        // Create a token without ctt header
        final String token = createTokenWithoutCtt();
        final String authHeader = "Bearer " + token;

        final Optional<OpAmpAuthContext> result = opAmpService.authenticate(authHeader, TRANSPORT);

        assertThat(result).isEmpty();
        verify(enrollmentTokenService, never()).validateToken(any());
        verify(agentTokenService, never()).validateAgentToken(any(), any());
    }

    @Test
    void authenticateReturnsEmptyForMissingBearer() {
        final String authHeader = "Basic dXNlcjpwYXNz";

        final Optional<OpAmpAuthContext> result = opAmpService.authenticate(authHeader, TRANSPORT);

        assertThat(result).isEmpty();
        verify(enrollmentTokenService, never()).validateToken(any());
        verify(agentTokenService, never()).validateAgentToken(any(), any());
    }

    @Test
    void authenticateReturnsEmptyForNullHeader() {
        final Optional<OpAmpAuthContext> result = opAmpService.authenticate(null, TRANSPORT);

        assertThat(result).isEmpty();
        verify(enrollmentTokenService, never()).validateToken(any());
        verify(agentTokenService, never()).validateAgentToken(any(), any());
    }

    @Test
    void authenticateReturnsEmptyForMalformedToken() {
        final String authHeader = "Bearer not.a.valid.jwt";

        final Optional<OpAmpAuthContext> result = opAmpService.authenticate(authHeader, TRANSPORT);

        assertThat(result).isEmpty();
        verify(enrollmentTokenService, never()).validateToken(any());
        verify(agentTokenService, never()).validateAgentToken(any(), any());
    }

    @Test
    void authenticateReturnsEmptyForInvalidBase64() {
        final String authHeader = "Bearer !!!invalid!!!.payload.signature";

        final Optional<OpAmpAuthContext> result = opAmpService.authenticate(authHeader, TRANSPORT);

        assertThat(result).isEmpty();
        verify(enrollmentTokenService, never()).validateToken(any());
        verify(agentTokenService, never()).validateAgentToken(any(), any());
    }

    // --- extractAttributes ---------------------------------------------------------------------
    //
    // See https://github.com/Graylog2/graylog2-server/issues/26901: Collector-submitted attributes
    // are untrusted input and must be bounded before persistence.

    @Test
    void extractAttributesSkipsBlankKeys() {
        final var input = List.of(
                stringKv("", "value-for-empty"),
                stringKv("   ", "value-for-whitespace"),
                stringKv("host.name", "host-1"));

        final var result = OpAmpService.extractAttributes("uid", 1L, input);

        assertThat(result).containsExactly(Attribute.of("host.name", "host-1"));
    }

    @Test
    void extractAttributesTruncatesLongKeys() {
        final String longKey = "k".repeat(OpAmpService.MAX_ATTRIBUTE_KEY_LENGTH + 50);
        final var input = List.of(stringKv(longKey, "value"));

        final var result = OpAmpService.extractAttributes("uid", 1L, input);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).key()).hasSize(OpAmpService.MAX_ATTRIBUTE_KEY_LENGTH);
        assertThat(result.get(0).key()).isEqualTo(longKey.substring(0, OpAmpService.MAX_ATTRIBUTE_KEY_LENGTH));
    }

    @Test
    void extractAttributesTruncatesLongStringValues() {
        final String longValue = "v".repeat(OpAmpService.MAX_ATTRIBUTE_VALUE_LENGTH + 50);
        final var input = List.of(stringKv("key", longValue));

        final var result = OpAmpService.extractAttributes("uid", 1L, input);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).value()).isEqualTo(longValue.substring(0, OpAmpService.MAX_ATTRIBUTE_VALUE_LENGTH));
    }

    @Test
    void extractAttributesTruncatesLongByteValues() {
        final ByteString longBytes = ByteString.copyFrom(new byte[OpAmpService.MAX_ATTRIBUTE_VALUE_LENGTH + 50]);
        final var input = List.of(Anyvalue.KeyValue.newBuilder()
                .setKey("key")
                .setValue(Anyvalue.AnyValue.newBuilder().setBytesValue(longBytes))
                .build());

        final var result = OpAmpService.extractAttributes("uid", 1L, input);

        assertThat(result).hasSize(1);
        assertThat(((ByteString) result.get(0).value()).size()).isEqualTo(OpAmpService.MAX_ATTRIBUTE_VALUE_LENGTH);
    }

    @Test
    void extractAttributesKeepsLastValueOnDuplicateKey() {
        final var input = List.of(
                stringKv("host.name", "first"),
                stringKv("host.name", "second"));

        final var result = OpAmpService.extractAttributes("uid", 1L, input);

        assertThat(result).containsExactly(Attribute.of("host.name", "second"));
    }

    @Test
    void extractAttributesDeduplicatesKeysThatOnlyCollideAfterTruncation() {
        final String prefix = "k".repeat(OpAmpService.MAX_ATTRIBUTE_KEY_LENGTH);
        final var input = List.of(
                stringKv(prefix + "-a", "first"),
                stringKv(prefix + "-b", "second"));

        final var result = OpAmpService.extractAttributes("uid", 1L, input);

        // Both keys truncate to the same MAX_ATTRIBUTE_KEY_LENGTH-char prefix, so this must collapse
        // to one entry with the later value, not throw or silently keep the first.
        assertThat(result).hasSize(1);
        assertThat(result.get(0).key()).hasSize(OpAmpService.MAX_ATTRIBUTE_KEY_LENGTH);
        assertThat(result.get(0).value()).isEqualTo("second");
    }

    @Test
    void extractAttributesDropsEntriesOverTheLimit() {
        final List<Anyvalue.KeyValue> input = new ArrayList<>();
        for (int i = 0; i < OpAmpService.MAX_ATTRIBUTES + 10; i++) {
            input.add(stringKv("key-" + i, "value-" + i));
        }

        final var result = OpAmpService.extractAttributes("uid", 1L, input);

        assertThat(result).hasSize(OpAmpService.MAX_ATTRIBUTES);
        // The first MAX_ATTRIBUTES entries are kept; later ones are dropped once the cap is hit.
        assertThat(result).extracting(Attribute::key).contains("key-0", "key-" + (OpAmpService.MAX_ATTRIBUTES - 1));
        assertThat(result).extracting(Attribute::key).doesNotContain("key-" + (OpAmpService.MAX_ATTRIBUTES + 5));
    }

    @Test
    void extractAttributesSkipsUnsupportedValueTypes() {
        final var arrayValue = Anyvalue.KeyValue.newBuilder()
                .setKey("array-attr")
                .setValue(Anyvalue.AnyValue.newBuilder()
                        .setArrayValue(Anyvalue.ArrayValue.newBuilder()
                                .addValues(Anyvalue.AnyValue.newBuilder().setStringValue("nested"))))
                .build();
        final var kvListValue = Anyvalue.KeyValue.newBuilder()
                .setKey("kvlist-attr")
                .setValue(Anyvalue.AnyValue.newBuilder()
                        .setKvlistValue(Anyvalue.KeyValueList.newBuilder()
                                .addValues(stringKv("nested-key", "nested-value"))))
                .build();
        final var unsetValue = Anyvalue.KeyValue.newBuilder()
                .setKey("unset-attr")
                .setValue(Anyvalue.AnyValue.newBuilder())
                .build();

        final var result = OpAmpService.extractAttributes("uid", 1L, List.of(arrayValue, kvListValue, unsetValue));

        assertThat(result).isEmpty();
    }

    @Test
    void extractAttributesExtractsAllSupportedScalarTypes() {
        final var input = List.of(
                stringKv("string-attr", "value"),
                Anyvalue.KeyValue.newBuilder().setKey("bool-attr")
                        .setValue(Anyvalue.AnyValue.newBuilder().setBoolValue(true)).build(),
                Anyvalue.KeyValue.newBuilder().setKey("int-attr")
                        .setValue(Anyvalue.AnyValue.newBuilder().setIntValue(42L)).build(),
                Anyvalue.KeyValue.newBuilder().setKey("double-attr")
                        .setValue(Anyvalue.AnyValue.newBuilder().setDoubleValue(3.14)).build());

        final var result = OpAmpService.extractAttributes("uid", 1L, input);

        assertThat(result).containsExactlyInAnyOrder(
                Attribute.of("string-attr", "value"),
                Attribute.of("bool-attr", true),
                Attribute.of("int-attr", 42L),
                Attribute.of("double-attr", 3.14));
    }

    private static Anyvalue.KeyValue stringKv(String key, String value) {
        return Anyvalue.KeyValue.newBuilder()
                .setKey(key)
                .setValue(Anyvalue.AnyValue.newBuilder().setStringValue(value))
                .build();
    }

    /**
     * Creates a JWT-like token with the specified ctt (custom token type) header.
     * This is not a valid signed JWT, just has the correct structure for header parsing.
     */
    private String createTokenWithCtt(String ctt) {
        final String header = f("{\"alg\":\"EdDSA\",\"ctt\":\"%s\",\"kid\":\"fingerprint\"}", ctt);
        final String payload = "{\"sub\":\"test\",\"exp\":9999999999}";
        final String signature = "signature";

        return Base64.getUrlEncoder().withoutPadding().encodeToString(header.getBytes(StandardCharsets.UTF_8))
                + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8))
                + "." + signature;
    }

    /**
     * Creates a JWT-like token without a ctt header.
     */
    private String createTokenWithoutCtt() {
        final String header = "{\"alg\":\"EdDSA\",\"kid\":\"fingerprint\"}";
        final String payload = "{\"sub\":\"test\",\"exp\":9999999999}";
        final String signature = "signature";

        return Base64.getUrlEncoder().withoutPadding().encodeToString(header.getBytes(StandardCharsets.UTF_8))
                + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8))
                + "." + signature;
    }
}
