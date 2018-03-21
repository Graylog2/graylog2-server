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
package org.graylog2.contentpacks;

import com.google.common.collect.ImmutableSet;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import org.apache.zookeeper.Op;
import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.contentpacks.model.ContentPack;
import org.graylog2.contentpacks.model.Identified;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.Revisioned;
import org.graylog2.database.MongoConnection;
import org.mongojack.Aggregation;
import org.mongojack.AggregationResult;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;
import java.util.Set;

@Singleton
public class ContentPackService {
    private static final String COLLECTION_NAME = "content_packs";

    private final JacksonDBCollection<ContentPack, ObjectId> dbCollection;
    private static final Logger LOG = LoggerFactory.getLogger(ContentPackService.class);

    @Inject
    public ContentPackService(final MongoJackObjectMapperProvider mapperProvider,
                              final MongoConnection mongoConnection) {
        this(JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection(COLLECTION_NAME),
                ContentPack.class, ObjectId.class, mapperProvider.get()));
    }

    ContentPackService(final JacksonDBCollection<ContentPack, ObjectId> dbCollection) {
        this.dbCollection = dbCollection;

        dbCollection.createIndex(new BasicDBObject(Identified.FIELD_META_ID, 1).append(Revisioned.FIELD_META_REVISION, 1), new BasicDBObject("unique", true));
    }

    public Set<ContentPack> loadAll() {
        final DBCursor<ContentPack> contentPacks = dbCollection.find();
        return ImmutableSet.copyOf((Iterable<ContentPack>) contentPacks);
    }

    public Set<ContentPack> loadAllLatest() {
        final DBObject groupStage = BasicDBObjectBuilder.start()
                .push("$group")
                .append("_id", "$id")
                .push("max_rev")
                .append("$max", "$rev")
                .get();
        final DBObject lookupStage = BasicDBObject.parse(
                "{$lookup: " +
                        "        {from: \"content_packs\",\n" +
                        "         let: {id:\"$_id\", rev:\"$max_rev\"},\n" +
                        "         pipeline: [\n" +
                        "             {$match:\n" +
                        "                 {$expr:\n" +
                        "                     {$and: [\n" +
                        "                         {$eq: [\"$id\",\"$$id\"]},\n" +
                        "                         {$eq: [\"$rev\", \"$$rev\"]}\n" +
                        "                     ]}\n" +
                        "                 }\n" +
                        "             }\n" +
                        "         ],\n" +
                        "         as: \"latest\"\n" +
                        "        }\n" +
                        "    }");
        final DBObject unwindStage = new BasicDBObject("$unwind", "$latest");
        final DBObject replaceRootStage = BasicDBObjectBuilder.start()
                .push("$replaceRoot")
                .append("newRoot", "$latest")
                .get();

        final AggregationResult<ContentPack> result = dbCollection.aggregate(new Aggregation<>(ContentPack.class,
                groupStage, lookupStage, unwindStage, replaceRootStage));
        return ImmutableSet.copyOf(result.results());
    }

    public Set<ContentPack> findAllById(ModelId id) {
        final DBCursor<ContentPack> result = dbCollection.find(DBQuery.is(Identified.FIELD_META_ID, id));
        return ImmutableSet.copyOf((Iterable<ContentPack>) result);
    }

    public Optional<ContentPack> findByIdAndRevision(ModelId id, int revision) {
        final DBQuery.Query query = DBQuery.is(Identified.FIELD_META_ID, id).is(Revisioned.FIELD_META_REVISION, revision);
        return Optional.ofNullable(dbCollection.findOne(query));
    }

    public Optional<ContentPack> insert(final ContentPack pack) {
        if (findByIdAndRevision(pack.id(), pack.revision()).isPresent()) {
            LOG.debug("Content pack already found: id: {} revision: {}. Did not insert!", pack.id(), pack.revision());
            return Optional.empty();
        }
        final WriteResult<ContentPack, ObjectId> writeResult = dbCollection.insert(pack);
        return Optional.of(writeResult.getSavedObject());
    }

    public int deleteById(ModelId id) {
        final DBQuery.Query query = DBQuery.is(Identified.FIELD_META_ID, id);
        final WriteResult<ContentPack, ObjectId> writeResult = dbCollection.remove(query);
        return writeResult.getN();
    }

    public void deleteByIdAndRevision(ModelId id, int revision) {
        final DBQuery.Query query = DBQuery.is(Identified.FIELD_META_ID, id).is(Revisioned.FIELD_META_REVISION, revision);
        dbCollection.remove(query);
    }
}
