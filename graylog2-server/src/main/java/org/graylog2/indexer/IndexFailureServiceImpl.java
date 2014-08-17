/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.indexer;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.database.CollectionName;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PersistedServiceImpl;
import org.joda.time.DateTime;

import java.util.List;

public class IndexFailureServiceImpl extends PersistedServiceImpl implements IndexFailureService {
    @Inject
    public IndexFailureServiceImpl(MongoConnection mongoConnection) {
        super(mongoConnection);

        // Make sure that the index failures collection is always created capped.
        final String collectionName = IndexFailureImpl.class.getAnnotation(CollectionName.class).value();
        if(!mongoConnection.getDatabase().collectionExists(collectionName)) {
            DBObject options = BasicDBObjectBuilder.start()
                    .add("capped", true)
                    .add("size", 52428800) // 50MB max size.
                    .get();

            mongoConnection.getDatabase().createCollection(collectionName, options);
        }
    }

    @Override
    public List<IndexFailure> all(int limit, int offset) {
        List<IndexFailure> failures = Lists.newArrayList();

        DBObject sort = new BasicDBObject();
        sort.put("$natural", -1);

        List<DBObject> results = query(IndexFailureImpl.class, new BasicDBObject(), sort, limit, offset);
        for (DBObject o : results) {
            failures.add(new IndexFailureImpl((ObjectId) o.get("_id"), o.toMap()));
        }

        return failures;
    }

    @Override
    public long countSince(DateTime since) {
        BasicDBObject query = new BasicDBObject();
        query.put("timestamp", new BasicDBObject("$gte", since.toDate()));

        return count(IndexFailureImpl.class, query);
    }

    @Override
    public long totalCount() {
        return collection(IndexFailureImpl.class).count();
    }
}