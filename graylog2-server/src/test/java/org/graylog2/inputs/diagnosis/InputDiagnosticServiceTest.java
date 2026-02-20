package org.graylog2.inputs.diagnosis;

import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.inputs.Input;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.rest.models.system.inputs.responses.InputDiagnostics;
import org.graylog2.rest.models.system.inputs.responses.InputDiagnostics.StreamMessageCount;
import org.graylog2.server.search.services.PivotSearchService;
import org.graylog2.streams.StreamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InputDiagnosticServiceTest {

    private PivotSearchService pivotSearchService;
    private StreamService streamService;
    private InputDiagnosticService inputDiagnosticService;

    @BeforeEach
    void setUp() {
        pivotSearchService = mock(PivotSearchService.class);
        streamService = mock(StreamService.class);
        inputDiagnosticService = new InputDiagnosticService(pivotSearchService, streamService);
    }

    @Test
    void returnsStreamMessageCountsFromPivot() throws Exception {
        final Input input = mock(Input.class);
        final SearchUser searchUser = mock(SearchUser.class);

        when(input.getId()).thenReturn("input-123");

        when(pivotSearchService.findPivotValues("gl2_source_input:input-123", "streams", searchUser))
                .thenReturn(Map.of("stream-1", 42L));

        final Stream stream = mock(Stream.class);
        when(stream.getId()).thenReturn("stream-1");
        when(stream.getTitle()).thenReturn("Stream One");

        when(streamService.load("stream-1")).thenReturn(stream);

        final InputDiagnostics result = inputDiagnosticService.getInputDiagnostics(input, searchUser);

        List<StreamMessageCount> streamMessageCounts = result.streamMessageCount(); // ← правильный метод

        assertThat(streamMessageCounts).hasSize(1);
        StreamMessageCount count = streamMessageCounts.get(0);
        assertThat(count.streamId()).isEqualTo("stream-1");
        assertThat(count.streamName()).isEqualTo("Stream One");
        assertThat(count.count()).isEqualTo(42L);
    }

    @Test
    void returnsEmptyDiagnosticsIfNoPivotResults() {
        final Input input = mock(Input.class);
        final SearchUser searchUser = mock(SearchUser.class);

        when(input.getId()).thenReturn("input-empty");

        when(pivotSearchService.findPivotValues("gl2_source_input:input-empty", "streams", searchUser))
                .thenReturn(Map.of());

        final InputDiagnostics result = inputDiagnosticService.getInputDiagnostics(input, searchUser);

        assertThat(result).isEqualTo(InputDiagnostics.EMPTY_DIAGNOSTICS);
    }
}
