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
package org.graylog.plugins.views.search.views.sharing;

import com.google.common.collect.ImmutableSet;
import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.mongojack.DBQuery;
import org.mongojack.JacksonDBCollection;

import javax.inject.Inject;
import java.util.Optional;
import java.util.Set;

public class ViewSharingService {
    protected final JacksonDBCollection<ViewSharing, ObjectId> db;

    @Inject
    public ViewSharingService(MongoConnection mongoConnection,
                              MongoJackObjectMapperProvider mapper) {
        this.db = JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection("view_sharings"),
                ViewSharing.class,
                org.bson.types.ObjectId.class,
                mapper.get());
    }

    public Set<ViewSharing> forViews(Set<String> viewIds) {
        return ImmutableSet.copyOf(this.db.find(DBQuery.in(ViewSharing.FIELD_VIEW_ID, viewIds)).iterator());
    }

    public Optional<ViewSharing> forView(String viewId) {
        return Optional.ofNullable(this.db.findOne(DBQuery.is(ViewSharing.FIELD_VIEW_ID, viewId)));
    }

    public ViewSharing create(ViewSharing viewSharing) {
        this.db.update(DBQuery.is(ViewSharing.FIELD_VIEW_ID, viewSharing.viewId()), viewSharing, true, false);
        return viewSharing;
    }

    public Optional<ViewSharing> remove(String viewId) {
        final ViewSharing viewSharing = this.db.findOne(DBQuery.is(ViewSharing.FIELD_VIEW_ID, viewId));
        this.db.remove(DBQuery.is(ViewSharing.FIELD_VIEW_ID, viewId));
        return Optional.ofNullable(viewSharing);
    }
}
