package org.graylog2.inputs.diagnosis;

import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.inputs.Input;
import org.graylog2.rest.models.system.inputs.responses.InputDiagnostics;
import org.graylog2.server.search.services.PivotSearchService;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class InputDiagnosticServiceTest {

    @Mock
    private PivotSearchService pivotSearchService;

    @Mock
    private StreamService streamService;

    @Mock
    private Input input;

    @Mock
    private SearchUser searchUser;

    @Mock
    private Stream stream;

    private InputDiagnosticService inputDiagnosticService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        inputDiagnosticService = new InputDiagnosticService(pivotSearchService, streamService);
    }

    @Test
    void testGetInputDiagnostics() throws Exception {
        when(input.getId()).thenReturn("input-id");
        when(stream.getId()).thenReturn("stream-id");
        when(stream.getTitle()).thenReturn("stream-title");
        when(streamService.load("stream-id")).thenReturn(stream);
        when(pivotSearchService.findPivotValues(anyString(), anyString(), any(SearchUser.class)))
            .thenReturn(Map.of("stream-id", 10L));

        final InputDiagnostics result = inputDiagnosticService.getInputDiagnostics(input, searchUser);

        assertEquals(1, result.streamMessageCounts().size());
        assertEquals("stream-id", result.streamMessageCounts().get(0).streamId());
        assertEquals("stream-title", result.streamMessageCounts().get(0).streamTitle());
        assertEquals(10L, result.streamMessageCounts().get(0).messageCount());
    }

    @Test
    void testGetInputDiagnosticsEmpty() {
        when(input.getId()).thenReturn("input-id");
        when(pivotSearchService.findPivotValues(anyString(), anyString(), any(SearchUser.class)))
            .thenReturn(Collections.emptyMap());

        final InputDiagnostics result = inputDiagnosticService.getInputDiagnostics(input, searchUser);

        assertEquals(0, result.streamMessageCounts().size());
    }
}
