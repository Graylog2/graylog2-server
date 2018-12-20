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

import org.graylog.plugins.sidecar.rest.models.CollectorUpload;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.database.PaginatedList;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ImportService extends PaginatedDbService<CollectorUpload> {
    private static final String COLLECTION_NAME = "collector_uploads";

    @Inject
    public ImportService(MongoConnection mongoConnection,
                         MongoJackObjectMapperProvider mapper){
        super(mongoConnection, mapper, CollectorUpload.class, COLLECTION_NAME);
    }

    public PaginatedList<CollectorUpload> findPaginated(int page, int perPage, String sortField, String order) {
        final DBSort.SortBuilder sortBuilder = getSortBuilder(order, sortField);
        return findPaginatedWithQueryAndSort(DBQuery.empty(), sortBuilder, page, perPage);
    }


    public List<CollectorUpload> all() {
        try (final Stream<CollectorUpload> collectorUploadStream = streamAll()) {
            return collectorUploadStream.collect(Collectors.toList());
        }
    }

    public long count() {
        return db.count();
    }

    public int destroyExpired(Period period) {
        final DateTime threshold = DateTime.now(DateTimeZone.UTC).minus(period);
        int count;

        try (final Stream<CollectorUpload> uploadStream = streamAll()) {
            count = uploadStream
                    .mapToInt(upload -> {
                        if (upload.created().isBefore(threshold)) {
                            return delete(upload.id());
                        }
                        return 0;
                    })
                    .sum();
        }

        return count;
    }
}
