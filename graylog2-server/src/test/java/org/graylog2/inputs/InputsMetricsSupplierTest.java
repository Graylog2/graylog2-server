package org.graylog2.inputs;

import org.graylog2.telemetry.scheduler.TelemetryEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class InputsMetricsSupplierTest {
    @Mock
    private InputService inputService;

    @Test
    public void shouldReturnCountsByType() {
        final Map<String, Long> counts = Map.of(
                "org.graylog.plugins.beats.Beats2Input", 2L,
                "org.graylog.plugins.forwarder.input.ForwarderServiceInput", 3L
        );
        when(inputService.totalCountByType()).thenReturn(counts);

        InputsMetricsSupplier supplier = new InputsMetricsSupplier(inputService);
        Optional<TelemetryEvent> event = supplier.get();

        assertTrue(event.isPresent());
        assertEquals(counts, event.get().metrics());
    }

    @Test
    public void shouldReturnEmptyMetricsWhenNoInputs() {
        when(inputService.totalCountByType()).thenReturn(Collections.emptyMap());

        InputsMetricsSupplier supplier = new InputsMetricsSupplier(inputService);
        Optional<TelemetryEvent> event = supplier.get();

        assertTrue(event.isPresent());
        assertTrue(event.get().metrics().isEmpty());
    }
}
