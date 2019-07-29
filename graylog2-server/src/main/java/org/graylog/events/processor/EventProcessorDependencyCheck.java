package org.graylog.events.processor;

import com.google.common.collect.ImmutableSet;
import org.graylog2.system.processing.DBProcessingStatusService;
import org.joda.time.DateTime;

import javax.inject.Inject;
import java.util.Set;

/**
 * This can be used by an event processor to check if required event definitions have already processed a specific
 * timerange.
 */
public class EventProcessorDependencyCheck {
    private final DBEventProcessorStateService stateService;
    private final DBProcessingStatusService processingStatusService;

    @Inject
    public EventProcessorDependencyCheck(DBEventProcessorStateService stateService,
                                         DBProcessingStatusService processingStatusService) {
        this.stateService = stateService;
        this.processingStatusService = processingStatusService;
    }

    /**
     * Checks if the given event definitions have already processed events up to the given timestamp.
     *
     * @param maxTimestamp          the max timestamp
     * @param processorDependencies set of event definition IDs
     * @return true if the given event definition IDs have processed events up to the given timestamp, false otherwise
     */
    public boolean canProcessTimerange(DateTime maxTimestamp, Set<String> processorDependencies) {
        final ImmutableSet<String> foundIds = stateService.findByEventDefinitionsAndMaxTimestamp(processorDependencies, maxTimestamp)
                .stream()
                .map(EventProcessorStateDto::eventDefinitionId)
                .collect(ImmutableSet.toImmutableSet());

        return foundIds.containsAll(processorDependencies);
    }

    /**
     * Checks if messages up to the given timestamp are searchable in Elasticsearch. It looks at the latest receive
     * timestamp that has been indexed by looking at the processing status maintained by
     * {@link org.graylog2.system.processing.ProcessingStatusRecorder}.
     * <p>
     * Caveat: This only looks at the processing status and doesn't take the Elasticsearch {@code index.refresh_interval}
     * into account!
     *
     * @param latestTimestamp the timestamp to check
     * @return true if messages up to the given latestTimestamp have already been indexed, false otherwise
     */
    public boolean hasMessagesIndexedUpTo(DateTime latestTimestamp) {
        return processingStatusService.earliestPostIndexingTimestamp()
                .map(latestPostIndexTimestamp -> latestPostIndexTimestamp.isAfter(latestTimestamp) || latestPostIndexTimestamp.isEqual(latestTimestamp))
                .orElse(false);
    }
}
