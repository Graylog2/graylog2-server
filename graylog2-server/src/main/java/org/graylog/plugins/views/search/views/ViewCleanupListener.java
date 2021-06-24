package org.graylog.plugins.views.search.views;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.graylog2.streams.events.StreamDeletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ViewCleanupListener {
    private static final Logger LOG = LoggerFactory.getLogger(ViewCleanupListener.class);

    private final ViewService viewService;

    @Inject
    public ViewCleanupListener(EventBus serverEventBus,
                                    ViewService viewService) {
        this.viewService = viewService;
        serverEventBus.register(this);
    }

    private Optional<ViewDTO> hasStream(final ViewDTO viewDTO, final String streamId) {
        return viewDTO.state().values().stream()
                .flatMap(s -> s.widgets().stream())
                .flatMap(s -> s.streams().stream())
                .anyMatch(s -> streamId.equals(s)) ? Optional.of(viewDTO) : Optional.empty();
    }

    @Subscribe
    @AllowConcurrentEvents
    public void handleStreamDeletion(StreamDeletedEvent event) {
        final String streamId = event.streamId();
        viewService
                .streamAll()
                .map(view -> this.hasStream(view, streamId))
                .filter(Optional::isPresent)
                .map(view -> {
                    ViewDTO viewDTO = view.get();
                    viewDTO.state().values().forEach(value -> {
                        value.widgets().forEach(widget -> {
                            widget.streams().remove(streamId);
                        });
                    });
                    return viewDTO;
                })
                .forEach(viewService::update);
    }
}
