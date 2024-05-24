package org.graylog.events.processor;

import jakarta.inject.Inject;
import org.graylog.plugins.views.search.searchfilters.db.SearchFiltersReFetcher;
import org.graylog.plugins.views.search.searchfilters.model.UsedSearchFilter;
import org.graylog2.database.PaginatedList;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class EventDefinitionService {
    private final DBEventDefinitionService dbEventDefinitionService;
    private final SearchFiltersReFetcher searchFiltersRefetcher;

    @Inject
    public EventDefinitionService(DBEventDefinitionService dbEventDefinitionService,
                                  SearchFiltersReFetcher searchFiltersRefetcher) {
        this.dbEventDefinitionService = dbEventDefinitionService;
        this.searchFiltersRefetcher = searchFiltersRefetcher;
    }

    public PaginatedList<EventDefinitionDto> searchPaginated(String query, Predicate<EventDefinitionDto> filter,
                                                             String sortByField, String sortOrder, int page, int perPage) {
        final PaginatedList<EventDefinitionDto> list = dbEventDefinitionService.searchPaginated(query, filter, sortByField, sortOrder, page, perPage);
        return new PaginatedList<>(
                list.stream()
                        .map(this::getEventDefinitionWithRefetchedFilters)
                        .collect(Collectors.toList()),
                list.pagination().total(),
                page,
                perPage
        );
    }

    public EventDefinitionDto save(final EventDefinitionDto entity) {
        return getEventDefinitionWithRefetchedFilters(dbEventDefinitionService.save(entity));
    }

    public Optional<EventDefinitionDto> get(String id) {
        return dbEventDefinitionService.get(id).map(this::getEventDefinitionWithRefetchedFilters);
    }

    public boolean isMutable(EventDefinitionDto eventDefinition) {
        return dbEventDefinitionService.isMutable(eventDefinition);
    }

    private EventDefinitionDto getEventDefinitionWithRefetchedFilters(final EventDefinitionDto eventDefinition) {
        final EventProcessorConfig config = eventDefinition.config();
        if (searchFiltersRefetcher.turnedOn() && config instanceof SearchFilterableConfig) {
            final List<UsedSearchFilter> filters = ((SearchFilterableConfig) config).filters();
            final EventProcessorConfig updatedConfig = config.updateFilters(searchFiltersRefetcher.reFetch(filters));
            if (updatedConfig == null) {
                return eventDefinition;
            }
            return eventDefinition.toBuilder().config(updatedConfig).build();
        }
        return eventDefinition;
    }
}
