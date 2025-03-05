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
package org.graylog2.inputs.codecs.gelf;

import org.graylog2.inputs.diagnosis.InputDiagnosisMetrics;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.plugin.system.SimpleNodeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static com.codahale.metrics.MetricRegistry.name;

@ExtendWith(MockitoExtension.class)
class GELFBulkDroppedMsgServiceTest {

    @Mock
    private InputDiagnosisMetrics inputDiagnosisMetrics;
    private GELFBulkDroppedMsgService classUnderTest;

    @BeforeEach
    void setUp() {
        classUnderTest = new GELFBulkDroppedMsgService(inputDiagnosisMetrics);
    }

    @Test
    void handleDroppedMsgOccurrence() {
        final String json = """
                {"short_message":"Bulk message 1", "host":"example.org", "facility":"test", "_foo":"bar"}

                {"short_message":"Bulk message 2", "host":"example.org", "facility":"test", "_foo":"bar"}
                """;

        final String gelfInputId = "gelfInputId";
        final RawMessage rawMessage = new RawMessage(json.getBytes(StandardCharsets.UTF_8));
        rawMessage.addSourceNode(gelfInputId, new SimpleNodeId("nodeId"));

        classUnderTest.handleDroppedMsgOccurrence(rawMessage);

        Mockito.verify(inputDiagnosisMetrics, Mockito.times(1)).incCount(name(GELFBulkDroppedMsgService.METRIC_PREFIX, gelfInputId, GELFBulkDroppedMsgService.METRIC_SUFFIX));
    }
}
