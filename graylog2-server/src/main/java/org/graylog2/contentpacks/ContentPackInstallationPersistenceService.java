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
import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.contentpacks.model.ContentPackInstallation;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.database.MongoConnection;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

@Singleton
public class ContentPackInstallationPersistenceService {
    private static final String COLLECTION_NAME = "content_packs_installations";

    private final JacksonDBCollection<ContentPackInstallation, ObjectId> dbCollection;

    @Inject
    public ContentPackInstallationPersistenceService(final MongoJackObjectMapperProvider mapperProvider,
                                                     final MongoConnection mongoConnection) {
        this(JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection(COLLECTION_NAME),
                ContentPackInstallation.class, ObjectId.class, mapperProvider.get()));
    }

    ContentPackInstallationPersistenceService(final JacksonDBCollection<ContentPackInstallation, ObjectId> dbCollection) {
        this.dbCollection = dbCollection;

        dbCollection.createIndex(new BasicDBObject(ContentPackInstallation.FIELD_CONTENT_PACK_ID, 1));
        dbCollection.createIndex(new BasicDBObject(ContentPackInstallation.FIELD_CONTENT_PACK_ID, 1).append(ContentPackInstallation.FIELD_CONTENT_PACK_REVISION, 1));
    }

    public Set<ContentPackInstallation> loadAll() {
        try (final DBCursor<ContentPackInstallation> installations = dbCollection.find()) {
            return ImmutableSet.copyOf((Iterator<ContentPackInstallation>) installations);
        }
    }

    public Optional<ContentPackInstallation> findById(ObjectId id) {
        final ContentPackInstallation installation = dbCollection.findOneById(id);
        return Optional.ofNullable(installation);
    }

    public Set<ContentPackInstallation> findByContentPackIdAndRevision(ModelId id, int revision) {
        final DBQuery.Query query = DBQuery
                .is(ContentPackInstallation.FIELD_CONTENT_PACK_ID, id)
                .is(ContentPackInstallation.FIELD_CONTENT_PACK_REVISION, revision);
        try (final DBCursor<ContentPackInstallation> installations = dbCollection.find(query)) {
            return ImmutableSet.copyOf((Iterator<ContentPackInstallation>) installations);
        }
    }

    public Set<ContentPackInstallation> findByContentPackId(ModelId id) {
        final DBQuery.Query query = DBQuery.is(ContentPackInstallation.FIELD_CONTENT_PACK_ID, id);
        try (final DBCursor<ContentPackInstallation> installations = dbCollection.find(query)) {
            return ImmutableSet.copyOf((Iterator<ContentPackInstallation>) installations);
        }
    }

    public Optional<ContentPackInstallation> insert(final ContentPackInstallation installation) {
        final WriteResult<ContentPackInstallation, ObjectId> writeResult = dbCollection.insert(installation);
        return Optional.of(writeResult.getSavedObject());
    }

    public int deleteById(ObjectId id) {
        final WriteResult<ContentPackInstallation, ObjectId> writeResult = dbCollection.removeById(id);
        return writeResult.getN();
    }
}
