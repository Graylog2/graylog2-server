package org.graylog2.lookup;

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
import java.util.stream.Collectors;

import javax.inject.Inject;

public class MongoLutService {

    private final JacksonDBCollection<LookupTableDto, ObjectId> db;

    @Inject
    public MongoLutService(MongoConnection mongoConnection,
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
        return asImmutableList(db.find(DBQuery.in("_id", cacheIds.stream().map(ObjectId::new).collect(Collectors.toList()))));
    }

    public Collection<LookupTableDto> findByDataAdapterIds(Collection<String> dataAdapterIds) {
        return asImmutableList(db.find(DBQuery.in("_id", dataAdapterIds.stream().map(ObjectId::new).collect(Collectors.toList()))));
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
}
