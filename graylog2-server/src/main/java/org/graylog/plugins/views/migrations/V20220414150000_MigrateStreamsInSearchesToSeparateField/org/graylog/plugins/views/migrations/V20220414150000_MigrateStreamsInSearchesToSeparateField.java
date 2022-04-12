package org.graylog.plugins.views.migrations.V20220414150000_MigrateStreamsInSearchesToSeparateField.org.graylog.plugins.views.migrations;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.bson.types.ObjectId;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog.plugins.views.search.Search;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.mongojack.DBCursor;
import org.mongojack.JacksonDBCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class V20220414150000_MigrateStreamsInSearchesToSeparateField extends Migration {

    private static final Logger LOG = LoggerFactory.getLogger(V20220414150000_MigrateStreamsInSearchesToSeparateField.class);

    private final ClusterConfigService clusterConfigService;
    private final JacksonDBCollection<Search, ObjectId> db;

    @Inject
    public V20220414150000_MigrateStreamsInSearchesToSeparateField(final MongoConnection mongoConnection,
                                                                   final ClusterConfigService clusterConfigService,
                                                                   final MongoJackObjectMapperProvider mapper) {
        this.db = JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection("searches"),
                Search.class,
                ObjectId.class,
                mapper.get());
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2022-04-14T15:00:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed.");
            return;
        }

        final StreamsInSearchMigration migration = new StreamsInSearchMigration();
        final MigrationStatistics topLevelMigrationStatistics = new MigrationStatistics();
        final Collection<String> failedToMigrate = new ArrayList<>();
        final DBCursor<Search> allSearches = db.find();
        for (final Search searchObject : allSearches) {

            final StreamsInSearchMigration.MigrationResponse migrationResponse = migration.migrateQueries(searchObject);

            if (migrationResponse.migrationStatistics.canBeUnchanged()) {
                topLevelMigrationStatistics.newUnchanged();
            } else if (migrationResponse.migrationStatistics.hasFilterError()) {
                //TODO: while it seems very unlikely that we will face filters that cannot be converted to streams... what to do with it, anyway?
                topLevelMigrationStatistics.newWrongFilter();
                failedToMigrate.add(searchObject.id());
            } else if (migrationResponse.migrationStatistics.canBeMigrated()) {
                topLevelMigrationStatistics.newMigrated();
                final Search updatedSearchObject = searchObject.toBuilder().queries(migrationResponse.updatedQuerySet).build();
                //db.update(DBQuery.is("_id", new ObjectId(searchObject.id())), updatedSearchObject); //TODO: won't change Mongo till the migration is fully finished
            }


        }
        summarizeMigration(topLevelMigrationStatistics, failedToMigrate);
        //TODO: won't confirm completion till the migration is fully finished
        //clusterConfigService.write(MigrationCompleted.create(updatedSearchIds));
    }

    private void summarizeMigration(final MigrationStatistics topLevelMigrationStatistics, final Collection<String> failedToMigrate) {
        LOG.info("Migration of streams from filter field in search document migrated " + topLevelMigrationStatistics.getNumMigrated() + " search documents");
        LOG.info("Migration of streams from filter field in search document haven't had to change " + topLevelMigrationStatistics.getNumUnchanged() + " search documents");
        LOG.warn("Migration of streams from filter field in search document was unsuccessful for " + topLevelMigrationStatistics.getNumWrongFilter() + " search documents:");
        failedToMigrate.forEach(failedToMigrateSearchID -> LOG.warn("Search document " + failedToMigrateSearchID));
    }


    @JsonAutoDetect
    @AutoValue
    @WithBeanGetter
    public static abstract class MigrationCompleted {
        @JsonProperty("searchIds")
        public abstract List<String> searchIds();

        @JsonCreator
        public static V20220414150000_MigrateStreamsInSearchesToSeparateField.MigrationCompleted create(@JsonProperty("searchIds") final List<String> searchIds) {
            return new AutoValue_V20220414150000_MigrateStreamsInSearchesToSeparateField_MigrationCompleted(searchIds);
        }
    }

}
