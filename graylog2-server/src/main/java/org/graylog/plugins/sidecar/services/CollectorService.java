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
package org.graylog.plugins.sidecar.services;

import org.graylog.plugins.sidecar.rest.models.Collector;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.database.PaginatedList;
import org.graylog2.search.SearchQuery;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class CollectorService extends PaginatedDbService<Collector> {
    private static final String COLLECTION_NAME = "sidecar_collectors";

    @Inject
    public CollectorService(MongoConnection mongoConnection,
                            MongoJackObjectMapperProvider mapper) {
        super(mongoConnection, mapper, Collector.class, COLLECTION_NAME);
    }

    public Collector find(String id) {
        return db.findOne(DBQuery.is("_id", id));
    }

    public Collector findByName(String name) {
        return db.findOne(DBQuery.is("name", name));
    }

    public List<Collector> allFilter(Predicate<Collector> filter) {
        try (final Stream<Collector> collectorsStream = streamAll()) {
            final Stream<Collector> filteredStream = filter == null ? collectorsStream : collectorsStream.filter(filter);
            return filteredStream.collect(Collectors.toList());
        }
    }

    public List<Collector> all() {
        return allFilter(null);
    }

    public PaginatedList<Collector> findPaginated(SearchQuery searchQuery, int page, int perPage, String sortField, String order) {
        final DBQuery.Query dbQuery = searchQuery.toDBQuery();
        final DBSort.SortBuilder sortBuilder = getSortBuilder(order, sortField);
        return findPaginatedWithQueryAndSort(dbQuery, sortBuilder, page, perPage);
    }

    public Collector fromRequest(Collector request) {
        return Collector.create(
                null,
                request.name(),
                request.serviceType(),
                request.nodeOperatingSystem(),
                request.executablePath(),
                request.configurationPath(),
                request.executeParameters(),
                request.validationCommand(),
                request.defaultTemplate());
    }

    public Collector fromRequest(String id, Collector request) {
        final Collector collector = fromRequest(request);
        return collector.toBuilder()
                .id(id)
                .build();
    }

    public Collector copy(String id, String name) {
        Collector collector = find(id);
        return collector.toBuilder()
                .id(null)
                .name(name)
                .build();
    }
}
