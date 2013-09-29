/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
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
 *
 */
package org.graylog2.notifications;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.Core;
import org.graylog2.database.Persisted;
import org.graylog2.database.ValidationException;
import org.graylog2.database.validators.Validator;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Notification extends Persisted {

    private static final Logger LOG = LoggerFactory.getLogger(Notification.class);

    public static final String COLLECTION = "notifications";

    public enum Type {
        DEFLECTOR_EXISTS_AS_INDEX,
        MULTI_MASTER,
        ES_OPEN_FILES
    }

    public enum Severity {
        NORMAL, URGENT
    }

    private final Type type;
    private final Severity severity;
    private final DateTime timestamp;

    protected Notification(ObjectId id, Core core, Map<String, Object> fields) {
        super(core, id, fields);

        this.type = Type.valueOf(((String) fields.get("type")).toUpperCase());
        this.severity = Severity.valueOf(((String) fields.get("severity")).toUpperCase());
        this.timestamp = new DateTime(fields.get("timestamp"));
    }

    protected Notification(Core core, Map<String, Object> fields) {
        super(core, fields);

        this.type = Type.valueOf(((String) fields.get("type")).toUpperCase());
        this.severity = Severity.valueOf(((String) fields.get("severity")).toUpperCase());
        this.timestamp = new DateTime(fields.get("timestamp"));
    }

    public static void publish(Core core, Type type, Severity severity) {
        // Write only if there is no such warning yet.
        if (!isFirst(core, type)) {
            return;
        }

        Map<String, Object> fields = Maps.newHashMap();
        fields.put("type", type.toString().toLowerCase());
        fields.put("severity", severity.toString().toLowerCase());
        fields.put("timestamp", Tools.iso8601());

        Notification w = new Notification(core, fields);

        try {
            w.save();
        } catch(ValidationException e) {
            // We have no validations, but just in case somebody adds some...
            LOG.error("Validating user warning failed.", e);
        }
    }

    public static void fixed(Core core, Type type) {
        destroy(new BasicDBObject("type", type.toString().toLowerCase()), core, COLLECTION);
    }

    public static boolean isFirst(Core core, Type type) {
        return (findOne(new BasicDBObject("type", type.toString().toLowerCase()), core, COLLECTION) == null);
    }

    public static List<Notification> all(Core core) {
        List<Notification> notifications = Lists.newArrayList();

        for(DBObject obj : query(new BasicDBObject(), new BasicDBObject("timestamp", -1), core, COLLECTION)) {
            try {
                notifications.add(new Notification((ObjectId) obj.get("_id"), core, obj.toMap()));
            } catch(IllegalArgumentException e) {
                LOG.warn("There is a notification type we can't handle: [" + obj.get("type") + "]");
                continue;
            }
        }

        return notifications;
    }

    @Override
    public ObjectId getId() {
        return this.id;
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    public Type getType() {
        return type;
    }

    public Severity getSeverity() {
        return severity;
    }

    @Override
    public String getCollectionName() {
        return COLLECTION;
    }

    @Override
    protected Map<String, Validator> getValidations() {
        return Maps.newHashMap();
    }

    @Override
    protected Map<String, Validator> getEmbeddedValidations(String key) {
        return Maps.newHashMap();
    }

}
