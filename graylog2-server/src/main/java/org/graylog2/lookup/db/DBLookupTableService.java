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
package org.graylog2.lookup.db;

import com.google.common.collect.ImmutableList;

import com.mongodb.BasicDBObject;

import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.lookup.dto.LookupTableDto;
import org.graylog2.rest.models.PaginatedList;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.inject.Inject;

public class DBLookupTableService {

    private final JacksonDBCollection<LookupTableDto, ObjectId> db;

    @Inject
    public DBLookupTableService(MongoConnection mongoConnection,
                                MongoJackObjectMapperProvider mapper) {

        db = JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection("lut_tables"),
                LookupTableDto.class,
                ObjectId.class,
                mapper.get());

        db.createIndex(new BasicDBObject("name", 1), new BasicDBObject("unique", true));
    }

    public Optional<LookupTableDto> get(String idOrName) {
        try {
            return Optional.ofNullable(db.findOneById(new ObjectId(idOrName)));
        } catch (IllegalArgumentException e) {
            // not an ObjectId, try again with name
            return Optional.ofNullable(db.findOne(DBQuery.is("name", idOrName)));

        }
    }

    public LookupTableDto save(LookupTableDto table) {
        WriteResult<LookupTableDto, ObjectId> save = db.save(table);
        return save.getSavedObject();
    }

    public Collection<LookupTableDto> findAll() {
        return asImmutableList(db.find());
    }

    public PaginatedList<LookupTableDto> findPaginated(DBQuery.Query query, DBSort.SortBuilder sort, int page, int perPage) {

        final DBCursor<LookupTableDto> cursor = db.find(query)
                .sort(sort)
                .limit(perPage)
                .skip(perPage * Math.max(0, page - 1));

        return new PaginatedList<>(asImmutableList(cursor), cursor.count(), page, perPage);
    }

    public Collection<LookupTableDto> findByCacheIds(Collection<String> cacheIds) {
        return asImmutableList(db.find(DBQuery.in("cache", cacheIds.stream().map(ObjectId::new).collect(Collectors.toList()))));
    }

    public Collection<LookupTableDto> findByDataAdapterIds(Collection<String> dataAdapterIds) {
        return asImmutableList(db.find(DBQuery.in("data_adapter", dataAdapterIds.stream().map(ObjectId::new).collect(Collectors.toList()))));
    }

    private ImmutableList<LookupTableDto> asImmutableList(Iterator<? extends LookupTableDto> cursor) {
        return ImmutableList.copyOf(cursor);
    }

    public void delete(String idOrName) {
        try {
            db.removeById(new ObjectId(idOrName));
        } catch (IllegalArgumentException e) {
            // not an ObjectId, try again with name
            db.remove(DBQuery.is("name", idOrName));
        }
    }

    public void forEach(Consumer<? super LookupTableDto> action) {
        db.find().forEachRemaining(action);
    }
}
