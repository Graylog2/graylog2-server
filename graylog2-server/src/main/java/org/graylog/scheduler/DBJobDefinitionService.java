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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.database.PaginatedList;
import org.mongojack.Aggregation;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

    /**
     * Returns all job definitions that have the given config field values, grouped by config field value.
     *
     * @param configField the config field
     * @param values      the values of the config field
     * @return the job definitions grouped by the given values
     */
    public Map<String, List<JobDefinitionDto>> getAllByConfigField(String configField, Collection<? extends Object> values) {
        final String field = String.format(Locale.US, "%s.%s", JobDefinitionDto.FIELD_CONFIG, configField);

        // Use aggregation to group job definitions by each config field value
        // Example aggregation output:
        //   {
        //     "config-field-value-1": [
        //       {JobDefinitionDto}, {JobDefinitionDto}
        //      ],
        //     "config-field-value-2": [
        //       {JobDefinitionDto}, {JobDefinitionDto}
        //      ]
        //   }
        final Aggregation.Pipeline<Void> pipeline = Aggregation.match(DBQuery.in(field, values))
                .group(field)
                .set(GroupAggregationResult.FIELD_VALUES, Aggregation.Group.list("$ROOT"))
                .sort(DBSort.asc("_id"));

        return db.aggregate(pipeline, GroupAggregationResult.class)
                .results()
                .stream()
                .collect(Collectors.toMap(GroupAggregationResult::id, GroupAggregationResult::values));
    }

    @AutoValue
    static abstract class GroupAggregationResult {
        static final String FIELD_VALUES = "values";

        @JsonProperty("_id")
        public abstract String id();

        @JsonProperty(FIELD_VALUES)
        public abstract List<JobDefinitionDto> values();

        @JsonCreator
        public static GroupAggregationResult create(@JsonProperty("_id") String id,
                                                    @JsonProperty(FIELD_VALUES) List<JobDefinitionDto> values) {
            return new AutoValue_DBJobDefinitionService_GroupAggregationResult(id, values);
        }
    }
}
