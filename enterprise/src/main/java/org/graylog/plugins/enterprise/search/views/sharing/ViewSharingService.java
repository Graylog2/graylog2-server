package org.graylog.plugins.enterprise.search.views.sharing;

import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.mongojack.DBQuery;
import org.mongojack.JacksonDBCollection;

import javax.inject.Inject;
import java.util.Optional;

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
