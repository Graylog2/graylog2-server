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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.mongojack.DBQuery;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;

/**
 * Manages the "index_field_types" MongoDB collection.
 */
public class IndexFieldTypesService {
    private static final String FIELDS_FIELD_NAMES = String.format(Locale.US, "%s.%s", IndexFieldTypesDTO.FIELD_FIELDS, FieldTypeDTO.FIELD_NAME);

    private final JacksonDBCollection<IndexFieldTypesDTO, ObjectId> db;

    @Inject
    public IndexFieldTypesService(MongoConnection mongoConnection,
                                  MongoJackObjectMapperProvider objectMapperProvider) {
        this.db = JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection("index_field_types"),
                IndexFieldTypesDTO.class,
                ObjectId.class,
                objectMapperProvider.get());

        this.db.createIndex(new BasicDBObject(ImmutableMap.of(
                IndexFieldTypesDTO.FIELD_INDEX_NAME, 1,
                IndexFieldTypesDTO.FIELD_INDEX_SET_ID, 1
        )), new BasicDBObject("unique", true));
        this.db.createIndex(new BasicDBObject(IndexFieldTypesDTO.FIELD_INDEX_NAME, 1), new BasicDBObject("unique", true));
        this.db.createIndex(new BasicDBObject(FIELDS_FIELD_NAMES, 1));
    }

    public Optional<IndexFieldTypesDTO> get(String idOrIndexName) {
        try {
            return Optional.ofNullable(db.findOneById(new ObjectId(idOrIndexName)));
        } catch (IllegalArgumentException e) {
            // Not an ObjectId, try again with index_name
            return Optional.ofNullable(db.findOne(DBQuery.is(IndexFieldTypesDTO.FIELD_INDEX_NAME, idOrIndexName)));
        }
    }

    public IndexFieldTypesDTO save(IndexFieldTypesDTO dto) {
        final WriteResult<IndexFieldTypesDTO, ObjectId> save = db.save(dto);
        return save.getSavedObject();
    }

    public Optional<IndexFieldTypesDTO> upsert(IndexFieldTypesDTO dto) {
        final WriteResult<IndexFieldTypesDTO, ObjectId> update = db.update(
                DBQuery.and(
                        DBQuery.is(IndexFieldTypesDTO.FIELD_INDEX_NAME, dto.indexName()),
                        DBQuery.is(IndexFieldTypesDTO.FIELD_INDEX_SET_ID, dto.indexSetId())
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
            db.remove(DBQuery.is(IndexFieldTypesDTO.FIELD_INDEX_NAME, idOrIndexName));
        }
    }

    public Collection<IndexFieldTypesDTO> findForIndexSet(String indexSetId) {
        return findByQuery(DBQuery.is(IndexFieldTypesDTO.FIELD_INDEX_SET_ID, indexSetId));
    }

    public Collection<IndexFieldTypesDTO> findForFieldNames(Collection<String> fieldNames) {
        return findByQuery(DBQuery.in(FIELDS_FIELD_NAMES, fieldNames));
    }

    public Collection<IndexFieldTypesDTO> findForFieldNamesAndIndices(Collection<String> fieldNames, Collection<String> indexNames) {
        final DBQuery.Query query = DBQuery.and(
                DBQuery.in(IndexFieldTypesDTO.FIELD_INDEX_NAME, indexNames),
                DBQuery.in(FIELDS_FIELD_NAMES, fieldNames)
        );

        return findByQuery(query);
    }

    public Collection<IndexFieldTypesDTO> findAll() {
        return findByQuery(DBQuery.empty());
    }

    private Collection<IndexFieldTypesDTO> findByQuery(DBQuery.Query query) {
        return ImmutableList.copyOf((Iterable<IndexFieldTypesDTO>) db.find(query));
    }
}
