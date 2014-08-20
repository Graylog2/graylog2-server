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
package org.graylog2.notifications;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.cluster.Node;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PersistedServiceImpl;
import org.graylog2.database.ValidationException;
import org.graylog2.plugin.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class NotificationServiceImpl extends PersistedServiceImpl implements NotificationService {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationServiceImpl.class);

    @Inject
    public NotificationServiceImpl(MongoConnection mongoConnection) {
        super(mongoConnection);
    }

    @Override
    public Notification build() {
        return new NotificationImpl();
    }

    @Override
    public Notification buildNow() {
        Notification notification = build();
        notification.addTimestamp(Tools.iso8601());

        return notification;
    }

    @Override
    public boolean fixed(NotificationImpl.Type type) {
        return fixed(type, null);
    }

    @Override
    public boolean fixed(NotificationImpl.Type type, Node node) {
        BasicDBObject qry = new BasicDBObject();
        qry.put("type", type.toString().toLowerCase());
        if (node != null)
            qry.put("node_id", node.getNodeId());
        return destroyAll(NotificationImpl.class, qry) > 0;
    }

    @Override
    public boolean isFirst(NotificationImpl.Type type) {
        return (findOne(NotificationImpl.class, new BasicDBObject("type", type.toString().toLowerCase())) == null);
    }

    @Override
    public List<Notification> all() {
        List<Notification> notifications = Lists.newArrayList();

        for (DBObject obj : query(NotificationImpl.class, new BasicDBObject(), new BasicDBObject("timestamp", -1))) {
            try {
                notifications.add(new NotificationImpl(new ObjectId(obj.get("_id").toString()), obj.toMap()));
            } catch (IllegalArgumentException e) {
                LOG.warn("There is a notification type we can't handle: [" + obj.get("type") + "]");
                continue;
            }
        }

        return notifications;
    }

    @Override
    public boolean publishIfFirst(Notification notification) {
        // node id should never be empty
        if (notification.getNodeId() == null) {
            addThisNode(notification);
        }

        // also the timestamp should never be empty
        if (notification.getTimestamp() == null) {
            notification.addTimestamp(Tools.iso8601());
        }

        // Write only if there is no such warning yet.
        if (!isFirst(notification.getType())) {
            return false;
        }
        try {
            save(notification);
        } catch(ValidationException e) {
            // We have no validations, but just in case somebody adds some...
            LOG.error("Validating user warning failed.", e);
            return false;
        }

        return true;
    }

    private Notification addThisNode(Notification notification) {
        return notification;
    }

    @Override
    public boolean fixed(Notification notification) {
        return fixed(notification.getType(), null);
    }

    @Override
    public int destroyAllByType(Notification.Type type) {
        return destroyAll(NotificationImpl.class, new BasicDBObject("type", type.toString().toLowerCase()));
    }
}