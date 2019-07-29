/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
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
