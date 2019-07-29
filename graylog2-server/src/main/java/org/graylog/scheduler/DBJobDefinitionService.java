package org.graylog.scheduler;

import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.database.PaginatedList;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;

import javax.inject.Inject;
import java.util.Locale;
import java.util.Optional;

public class DBJobDefinitionService extends PaginatedDbService<JobDefinitionDto> {
    private static final String COLLECTION_NAME = "scheduler_job_definitions";

    @Inject
    public DBJobDefinitionService(MongoConnection mongoConnection, MongoJackObjectMapperProvider mapper) {
        super(mongoConnection, mapper, JobDefinitionDto.class, COLLECTION_NAME);
    }

    public PaginatedList<JobDefinitionDto> getAllPaginated(String sortByField, int page, int perPage) {
        return findPaginatedWithQueryAndSort(DBQuery.empty(), DBSort.asc(sortByField), page, perPage);
    }

    /**
     * Returns the job definition that has the given config field value.
     *
     * @param configField the config field
     * @param value       the value of the config field
     * @return the job definition with the given config field, or an empty optional
     */
    public Optional<JobDefinitionDto> getByConfigField(String configField, Object value) {
        final String field = String.format(Locale.US, "%s.%s", JobDefinitionDto.FIELD_CONFIG, configField);
        return Optional.ofNullable(db.findOne(DBQuery.is(field, value)));
    }
}
