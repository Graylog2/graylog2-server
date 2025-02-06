package org.graylog2.inputs.diagnosis;

import jakarta.inject.Inject;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

public class InputDiagnosisMetricsPeriodical extends Periodical {

    private static final Logger LOG = LoggerFactory.getLogger(InputDiagnosisMetricsPeriodical.class);

    private final InputDiagnosisMetrics inputDiagnosisMetrics;

    @Inject
    public InputDiagnosisMetricsPeriodical(InputDiagnosisMetrics inputDiagnosisMetrics) {
        this.inputDiagnosisMetrics = inputDiagnosisMetrics;
    }

    @Override
    public boolean runsForever() {
        return false;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return true;
    }

    @Override
    public boolean startOnThisNode() {
        return true;
    }

    @Override
    public boolean isDaemon() {
        return true;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 60;
    }

    @Override
    public int getPeriodSeconds() {
        return 60;
    }

    @Override
    protected @Nonnull Logger getLogger() {
        return LOG;
    }

    @Override
    public void doRun() {
        inputDiagnosisMetrics.update();
    }
}
