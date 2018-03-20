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

import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.collect.Iterators;
import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.contentpacks.model.ContentPack;
import org.graylog2.contentpacks.model.ContentPackView;
import org.graylog2.database.MongoConnection;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashSet;
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

    public ContentPackService(final JacksonDBCollection<ContentPack, ObjectId> dbCollection) {
        this.dbCollection = dbCollection;
    }

    public Set<ContentPack> loadAll() {
        final DBCursor<ContentPack> ContentPacks = dbCollection.find();
        final Set<ContentPack> contentPacks = new HashSet<>();

        Iterators.addAll(contentPacks, ContentPacks);

        return contentPacks;
    }

    @JsonView(ContentPackView.DBView.class)
    public ContentPack insert(final ContentPack pack) {
        final DBQuery.Query query = DBQuery.is("id", pack.id()).is("revision", pack.revision());
        if (dbCollection.findOne(query) != null) {
            LOG.debug("Content pack already found id: {} revision: {}. Did not insert!", pack.id(), pack.revision());
            return null;
        }
        final WriteResult<ContentPack, ObjectId> writeResult = dbCollection.insert(pack);
        return writeResult.getSavedObject();
    }
}
