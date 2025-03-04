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

    private final InputDiagnosisMetrics inputDiagnosisMetrics;

    @Inject
    public GELFBulkDroppedMsgService(InputDiagnosisMetrics inputDiagnosisMetrics) {
        this.inputDiagnosisMetrics = inputDiagnosisMetrics;
    }

    public void handleDroppedMsgOccurrence(RawMessage rawMessage) {
        final String inputIdOnCurrentNode = InputDiagnosisMetrics.getInputIOnCurrentNode(rawMessage);
        inputDiagnosisMetrics.incCount(name("org.graylog2.inputs", inputIdOnCurrentNode, "dropped.message.occurrence"));
        LOG.warn("Unexpected additional JSON content encountered after the initial valid JSON for GELF input id: {}. To ensure complete data processing, please enable bulk receiving.", inputIdOnCurrentNode);
    }
}
