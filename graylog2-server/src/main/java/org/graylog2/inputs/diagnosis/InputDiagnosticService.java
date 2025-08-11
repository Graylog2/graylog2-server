package org.graylog2.inputs.diagnosis;

import jakarta.inject.Inject;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.database.NotFoundException;
import org.graylog2.inputs.Input;
import org.graylog2.rest.models.system.inputs.responses.InputDiagnostics;
import org.graylog2.server.search.services.PivotSearchService;
import org.graylog2.streams.StreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.graylog2.plugin.Message.FIELD_GL2_SOURCE_INPUT;
import static org.graylog2.rest.models.system.inputs.responses.InputDiagnostics.EMPTY_DIAGNOSTICS;
import static org.graylog2.rest.models.system.inputs.responses.InputDiagnostics.StreamMessageCount;

public class InputDiagnosticService {
    private static final Logger LOG = LoggerFactory.getLogger(InputDiagnosticService.class);

    private final PivotSearchService pivotSearchService;
    private final StreamService streamService;

    @Inject
    public InputDiagnosticService(PivotSearchService pivotSearchService,
                                  StreamService streamService) {
        this.pivotSearchService = pivotSearchService;
        this.streamService = streamService;
    }

    public InputDiagnostics getInputDiagnostics(
            Input input, SearchUser searchUser) {
        final Map<String, Long> streamCounts = pivotSearchService.findPivotValues(
            FIELD_GL2_SOURCE_INPUT + ":" + input.getId(),
            "streams",
            searchUser
        );

        if (streamCounts.isEmpty()) {
            return EMPTY_DIAGNOSTICS;
        }

        final List<StreamMessageCount> streamMessageCounts = new ArrayList<>();
        streamCounts.forEach((streamId, count) -> {
            try {
                final org.graylog2.plugin.streams.Stream stream = streamService.load(streamId);
                streamMessageCounts.add(new StreamMessageCount(stream.getTitle(), stream.getId(), count));
            } catch (NotFoundException e) {
                LOG.warn("Unable to load stream {}", streamId, e);
            }
        });

        return new InputDiagnostics(streamMessageCounts);
    }
}
