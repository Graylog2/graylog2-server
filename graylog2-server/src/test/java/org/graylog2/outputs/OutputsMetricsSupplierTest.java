package org.graylog2.outputs;

import org.graylog2.streams.OutputService;
import org.graylog2.telemetry.scheduler.TelemetryEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutputsMetricsSupplierTest {

    @Mock
    private OutputService outputService;

    @InjectMocks
    private OutputsMetricsSupplier testee;

    @Test
    void emptyServiceResponseReturnsEmptyOptional() {
        when(outputService.countByType()).thenReturn(Collections.emptyMap());

        assertThat(testee.get()).isEmpty();

        verify(outputService).countByType();
        verifyNoMoreInteractions(outputService);
    }

    @Test
    void outputTypesFrequencyGetsWrappedInTelemetryEvent() {
        final Map<String, Long> typeFrequency = Map.of("a", 1L, "b", 2L, "c", 3L);
        when(outputService.countByType()).thenReturn(typeFrequency);

        // Yes, duplicating the typeFrequency-map from above, but this time, it's not Map<String, Long>, but Map<String, Object>...
        final Optional<TelemetryEvent> expected = Optional.of(
                TelemetryEvent.of(Map.of("a", 1L, "b", 2L, "c", 3L))
        );

        assertThat(testee.get()).isEqualTo(expected);
        verify(outputService).countByType();
        verifyNoMoreInteractions(outputService);
    }
}
