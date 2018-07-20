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
package org.graylog2.migrations;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.graylog2.contentpacks.ContentPackPersistenceService;
import org.graylog2.contentpacks.model.ContentPack;
import org.graylog2.database.MongoConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Filters.not;
import static com.mongodb.client.model.Filters.or;

/**
 * Migration adding mandatory (due to unique index) "id" and "rev" fields to legacy content packs.
 */
public class V20180718155800_AddContentPackIdAndRev extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20180718155800_AddContentPackIdAndRev.class);

    private final MongoCollection<Document> collection;

    @Inject
    public V20180718155800_AddContentPackIdAndRev(MongoConnection mongoConnection) {
        this(mongoConnection.getMongoDatabase().getCollection(ContentPackPersistenceService.COLLECTION_NAME));
    }

    private V20180718155800_AddContentPackIdAndRev(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2018-07-18T15:58:00Z");
    }

    @Override
    public void upgrade() {
        final FindIterable<Document> documentsWithMissingFields = collection.find(or(not(exists(ContentPack.FIELD_META_ID)), not(exists(ContentPack.FIELD_META_REVISION))));
        for (Document document : documentsWithMissingFields) {
            final ObjectId objectId = document.getObjectId("_id");
            LOG.debug("Found document with missing \"id\" or \"rev\" field with ID <{}>", objectId);
            final String id = document.get("id", objectId.toHexString());
            final int rev = document.get("rev", 0);

            document.put("id", id);
            document.put("rev", rev);

            final UpdateResult updateResult = collection.replaceOne(eq("_id", objectId), document);

            if (updateResult.wasAcknowledged()) {
                LOG.debug("Successfully updated document with ID <{}>", objectId);
            } else {
                LOG.error("Failed to update document with ID <{}>", objectId);
            }
        }
    }
}
