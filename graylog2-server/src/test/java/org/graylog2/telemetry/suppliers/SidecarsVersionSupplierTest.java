package org.graylog2.telemetry.suppliers;

import org.graylog.plugins.sidecar.services.SidecarService;
import org.graylog2.telemetry.scheduler.TelemetryEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SidecarsVersionSupplierTest {
    @Mock
    private SidecarService sidecarService;

    @InjectMocks
    private SidecarsVersionSupplier supplier;

    @Test
    public void shouldReturnSidecarsVersion() {
        final Map<String, Long> counts = Map.of(
                "1.5.1", 3L,
                "1.4.0", 2L
        );

        when(sidecarService.countByVersion()).thenReturn(counts);

        Optional<TelemetryEvent> event = supplier.get();

        assertThat(event).isPresent();
        assertThat(event.get().metrics())
                .isEqualTo(Map.of(
                        "1.5.1", 3L,
                        "1.4.0", 2L
                ));
    }
}
