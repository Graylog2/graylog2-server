package org.graylog.events.processor.aggregation;

public interface AggregationSearch {
    interface Factory {
        AggregationSearch create(AggregationEventProcessorConfig config,
                                 AggregationEventProcessorParameters parameters,
                                 String searchOwner);
    }

    AggregationResult doSearch();
}
