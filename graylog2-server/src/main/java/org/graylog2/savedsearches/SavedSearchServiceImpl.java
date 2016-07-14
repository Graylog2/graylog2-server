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
package org.graylog2.savedsearches;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.PersistedServiceImpl;
import org.graylog2.plugin.database.ValidationException;
import org.joda.time.DateTime;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

public class SavedSearchServiceImpl extends PersistedServiceImpl implements SavedSearchService {
    @Inject
    public SavedSearchServiceImpl(MongoConnection mongoConnection) {
        super(mongoConnection);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SavedSearch> all() {
        List<SavedSearch> searches = Lists.newArrayList();

        List<DBObject> results = query(SavedSearchImpl.class, new BasicDBObject());
        for (DBObject o : results) {

            searches.add(new SavedSearchImpl((ObjectId) o.get("_id"), o.toMap()));
        }

        return searches;
    }

    @Override
    @SuppressWarnings("unchecked")
    public SavedSearch load(String id) throws NotFoundException {
        BasicDBObject o = (BasicDBObject) get(SavedSearchImpl.class, id);

        if (o == null) {
            throw new NotFoundException("Couldn't find saved search with ID " + id);
        }

        return new SavedSearchImpl((ObjectId) o.get("_id"), o.toMap());
    }

    @Override
    public SavedSearch create(String title, Map<String, Object> query, String creatorUserId, DateTime createdAt) {
        // Create saved search
        final Map<String, Object> searchData = ImmutableMap.of(
                SavedSearchImpl.FIELD_TITLE, title,
                SavedSearchImpl.FIELD_QUERY, query,
                SavedSearchImpl.FIELD_CREATOR_USER_ID, creatorUserId,
                SavedSearchImpl.FIELD_CREATED_AT, createdAt);

        return new SavedSearchImpl(searchData);
    }

    @Override
    public void update(SavedSearch search, String title, Map<String, Object> query) throws ValidationException {
        if (title != null) {
            search.getFields().put(SavedSearchImpl.FIELD_TITLE, title);
        }

        if (query != null) {
            search.getFields().put(SavedSearchImpl.FIELD_QUERY, query);
        }

        save(search);
    }
}
