package org.graylog2.telemetry.scheduler;

import org.graylog2.configuration.TelemetryConfiguration;
import org.graylog2.telemetry.client.TelemetryClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelemetrySubmissionPeriodicalTest {

    @Mock
    private TelemetryClient client;

    private TelemetrySubmissionPeriodical testee;

    @Test
    void emptyProviderResponseDoesntCallTheClient() {
        TelemetryConfiguration conf = mock(TelemetryConfiguration.class);
        TelemetryMetricSupplier supplier1 = mock(TelemetryMetricSupplier.class);
        TelemetryMetricSupplier supplier2 = mock(TelemetryMetricSupplier.class);
        when(conf.isTelemetryEnabled()).thenReturn(true);

        final Map<String, TelemetryMetricSupplier> providers = Map.of("1", supplier1, "2", supplier2);
        when(supplier1.get()).thenReturn(Optional.empty());
        when(supplier2.get()).thenReturn(Optional.of(TelemetryEvent.of(Map.of())));

        testee = new TelemetrySubmissionPeriodical(client, conf, providers);

        testee.doRun();

        verifyNoInteractions(client);
    }

    @Test
    void reportedMetricsCallTheClient() throws IOException {
        TelemetryConfiguration conf = mock(TelemetryConfiguration.class);
        TelemetryMetricSupplier supplier1 = mock(TelemetryMetricSupplier.class);
        TelemetryMetricSupplier supplier2 = mock(TelemetryMetricSupplier.class);
        when(conf.isTelemetryEnabled()).thenReturn(true);

        final Map<String, TelemetryMetricSupplier> providers = Map.of("1", supplier1, "2", supplier2);
        final TelemetryEvent event1 = TelemetryEvent.of(Map.of("1a", 1));
        final TelemetryEvent event2 = TelemetryEvent.of(Map.of("2a", 2, "2b", 3));
        when(supplier1.get()).thenReturn(Optional.of(event1));
        when(supplier2.get()).thenReturn(Optional.of(event2));

        testee = new TelemetrySubmissionPeriodical(client, conf, providers);

        testee.doRun();

        verify(client).capture(Map.of("1", event1, "2", event2));
    }
}
