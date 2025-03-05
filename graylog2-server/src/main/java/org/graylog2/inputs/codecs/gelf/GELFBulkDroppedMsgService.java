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

import com.swrve.ratelimitedlogger.RateLimitedLog;
import jakarta.inject.Inject;
import org.graylog2.inputs.diagnosis.InputDiagnosisMetrics;
import org.graylog2.plugin.journal.RawMessage;

import java.time.Duration;

import static com.codahale.metrics.MetricRegistry.name;
import static org.graylog2.plugin.utilities.ratelimitedlog.RateLimitedLogFactory.createRateLimitedLog;

public class GELFBulkDroppedMsgService {
    private static final RateLimitedLog LOG = createRateLimitedLog(GELFBulkDroppedMsgService.class, 1, Duration.ofSeconds(5));
    public static final String METRIC_SUFFIX = "dropped.message.occurrence";
    protected static final String METRIC_PREFIX = "org.graylog2.inputs";

    private final InputDiagnosisMetrics inputDiagnosisMetrics;

    @Inject
    public GELFBulkDroppedMsgService(InputDiagnosisMetrics inputDiagnosisMetrics) {
        this.inputDiagnosisMetrics = inputDiagnosisMetrics;
    }

    public void handleDroppedMsgOccurrence(RawMessage rawMessage) {
        final String inputIdOnCurrentNode = InputDiagnosisMetrics.getInputIOnCurrentNode(rawMessage);
        inputDiagnosisMetrics.incCount(name(METRIC_PREFIX, inputIdOnCurrentNode, METRIC_SUFFIX));
        LOG.warn("Unexpected additional JSON content encountered after the initial valid JSON for GELF input id: {}. To ensure complete data processing, please enable bulk receiving.", inputIdOnCurrentNode);
    }
}
