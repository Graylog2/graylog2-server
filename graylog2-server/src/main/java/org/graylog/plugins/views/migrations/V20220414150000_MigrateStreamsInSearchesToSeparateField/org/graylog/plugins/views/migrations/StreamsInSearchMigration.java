package org.graylog.plugins.views.migrations.V20220414150000_MigrateStreamsInSearchesToSeparateField.org.graylog.plugins.views.migrations;

import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;

public class StreamsInSearchMigration {

    MigrationResponse migrateQueries(final Search search) {

        final StreamsInQueryMigration queryMigration = new StreamsInQueryMigration();
        final ImmutableSet<Query> updatedQuerySet = search.queries()
                .stream()
                .map(queryMigration::migrate)
                .collect(ImmutableSet.toImmutableSet());
        return new MigrationResponse(queryMigration.getMigrationStatistics(), updatedQuerySet);
    }


    static class MigrationResponse {
        final MigrationStatistics migrationStatistics;
        final ImmutableSet<Query> updatedQuerySet;

        public MigrationResponse(final MigrationStatistics migrationStatistics, final ImmutableSet<Query> updatedQuerySet) {
            this.migrationStatistics = migrationStatistics;
            this.updatedQuerySet = updatedQuerySet;
        }
    }
}
