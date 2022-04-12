package org.graylog.plugins.views.migrations.V20220414150000_MigrateStreamsInSearchesToSeparateField.org.graylog.plugins.views.migrations;

import org.graylog.plugins.views.search.Filter;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.filter.OrFilter;
import org.graylog.plugins.views.search.filter.StreamFilter;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class StreamsInQueryMigration {

    private MigrationStatistics migrationStatistics = new MigrationStatistics();

    Query migrate(final Query original) {

        if (!needsMigration(original)) {
            migrationStatistics.newUnchanged();
            return original;
        } else {
            //TODO: we may try to consider Query#usedStreamIds(), but it seems to get to deep with filter analysys
            final Filter filter = original.filter();
            if (filter instanceof OrFilter) {
                final Set<Filter> subFilters = filter.filters();
                if (subFilters.stream().allMatch(sf -> sf instanceof StreamFilter)) {
                    final Set<String> streamIds = fetchStreamIdsFromFilters(subFilters);
                    migrationStatistics.newMigrated();
                    return original.toBuilder()
                            .filter(null)
                            .streams(streamIds)
                            .build();
                }
            }
            migrationStatistics.newWrongFilter();
            return original;
        }
    }

    private Set<String> fetchStreamIdsFromFilters(final Collection<Filter> subFilters) {
        return subFilters.stream()
                .map(subFilter -> (StreamFilter) subFilter)
                .map(StreamFilter::streamId)
                .collect(Collectors.toSet());
    }

    private boolean needsMigration(final Query searchQuery) {
        final Filter filter = searchQuery.filter();
        return filter != null && filter.filters() != null && !filter.filters().isEmpty();
    }

    public MigrationStatistics getMigrationStatistics() {
        return migrationStatistics;
    }
}
