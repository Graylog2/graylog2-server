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
package org.graylog2.indexer.fieldtypes;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Streams;
import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.mongojack.DBQuery;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;

import javax.inject.Inject;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Manages the "index_field_Types" MongoDB collection.
 */
public class IndexFieldTypesService {

    private final JacksonDBCollection<IndexFieldTypes, ObjectId> db;

    @Inject
    public IndexFieldTypesService(MongoConnection mongoConnection,
                                  MongoJackObjectMapperProvider objectMapperProvider) {
        this.db = JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection("index_field_types"),
                IndexFieldTypes.class,
                ObjectId.class,
                objectMapperProvider.get());

        // TODO: Check which fields need an index!
        //       - indexName
        //       - fields.fieldName? fields[].fieldName?
        //this.db.createIndex(new BasicDBObject(IndexFieldTypesDTO.FIELD_INDEX_NAME, 1), new BasicDBObject("unique", true));
    }

    public Optional<IndexFieldTypes> get(String idOrIndexName) {
        try {
            return Optional.ofNullable(db.findOneById(new ObjectId(idOrIndexName)));
        } catch (IllegalArgumentException e) {
            // Not an ObjectId, try again with index_name
            return Optional.ofNullable(db.findOne(DBQuery.is(IndexFieldTypes.FIELD_INDEX_NAME, idOrIndexName)));
        }
    }

    public IndexFieldTypes save(IndexFieldTypes dto) {
        final WriteResult<IndexFieldTypes, ObjectId> save = db.save(dto);
        return save.getSavedObject();
    }

    public Optional<IndexFieldTypes> upsert(IndexFieldTypes dto) {
        final WriteResult<IndexFieldTypes, ObjectId> update = db.update(
                DBQuery.and(
                        DBQuery.is(IndexFieldTypes.FIELD_INDEX_NAME, dto.indexName()),
                        DBQuery.is(IndexFieldTypes.FIELD_INDEX_SET_ID, dto.indexSetId())
                ),
                dto,
                true,
                false
        );

        final Object upsertedId = update.getUpsertedId();
        if (upsertedId instanceof ObjectId) {
            return get(((ObjectId) upsertedId).toHexString());
        } else if (upsertedId instanceof String) {
            return get((String) upsertedId);
        }
        return Optional.empty();
    }

    public void delete(String idOrIndexName) {
        try {
            db.removeById(new ObjectId(idOrIndexName));
        } catch (IllegalArgumentException e) {
            // Not an ObjectId, try again with index_name
            db.remove(DBQuery.is(IndexFieldTypes.FIELD_INDEX_NAME, idOrIndexName));
        }
    }

    public Stream<IndexFieldTypes> streamForIndexSet(String indexSetId) {
        return Streams.stream((Iterable<IndexFieldTypes>) db.find(DBQuery.is(IndexFieldTypes.FIELD_INDEX_SET_ID, indexSetId)));
    }

    public Stream<IndexFieldTypes> streamAll() {
        return Streams.stream((Iterable<IndexFieldTypes>) db.find());
    }
}
