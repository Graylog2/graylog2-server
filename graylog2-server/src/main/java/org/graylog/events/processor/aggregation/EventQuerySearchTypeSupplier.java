package org.graylog.events.processor.aggregation;

import org.graylog.events.processor.EventDefinition;
import org.graylog.plugins.views.search.SearchType;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Set;

/**
 * Implementations of this interface are able to contribute {@link org.graylog.plugins.views.search.SearchType search types}
 * to non-scroll-based event processors.
 * <p>
 * The search types will be added to the original query and their results will be available to {@link org.graylog.events.processor.modifier.EventModifier event modifiers} by the engine.
 */
public interface EventQuerySearchTypeSupplier {
    @Nonnull
    Set<SearchType> additionalSearchTypes(EventDefinition eventDefinition);

    @Nonnull
    Map<String, Object> eventModifierData(Map<String, SearchType.Result> results);
}
