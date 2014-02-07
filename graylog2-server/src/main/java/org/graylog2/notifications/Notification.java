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
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.database.Persisted;
import org.graylog2.database.ValidationException;
import org.graylog2.database.validators.Validator;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
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
        NO_MASTER,
        ES_OPEN_FILES,
        NO_INPUT_RUNNING,
        INPUT_FAILED_TO_START,
        CHECK_SERVER_CLOCKS,
        OUTDATED_VERSION
    }

    public enum Severity {
        NORMAL, URGENT
    }

    private Type type;
    private Severity severity;
    private DateTime timestamp;
    private String node_id;

    protected Notification(ObjectId id, Core core, Map<String, Object> fields) {
        super(core, id, fields);

        this.type = Type.valueOf(((String) fields.get("type")).toUpperCase());
        this.severity = Severity.valueOf(((String) fields.get("severity")).toUpperCase());
        this.timestamp = new DateTime(fields.get("timestamp"));
        this.node_id = (String)fields.get("node_id");
    }

    protected Notification(Core core, Map<String, Object> fields) {
        super(core, fields);

        this.type = Type.valueOf(((String) fields.get("type")).toUpperCase());
        this.severity = Severity.valueOf(((String) fields.get("severity")).toUpperCase());
        this.timestamp = new DateTime(fields.get("timestamp"));
        this.node_id = (String)fields.get("node_id");
    }

    public Notification(Core core) {
        super(core, new HashMap<String, Object>());
    }

    public static Notification build(Core core) {
        return new Notification(core);
    }

    public static Notification buildNow(Core core) {
        Notification notification = build(core);
        notification.addTimestamp(Tools.iso8601());

        return notification;
    }

    public Notification addType(Type type) {
        this.type = type;
        fields.put("type", type.toString().toLowerCase());
        return this;
    }

    public Notification addTimestamp(DateTime timestamp) {
        this.timestamp = timestamp;
        fields.put("timestamp", Tools.getISO8601String(timestamp));

        return this;
    }

    public Notification addSeverity(Severity severity) {
        this.severity = severity;
        fields.put("severity", severity.toString().toLowerCase());
        return this;
    }

    public Notification addNode(Node node) {
        fields.put("node_id", node.getNodeId());
        return this;
    }

    public Notification addThisNode() {
        Node node;
        try {
            node = Node.thisNode(this.core);
        } catch (NodeNotFoundException e) {
            return this;
        }

        addNode(node);

        return this;
    }

    public boolean publishIfFirst() {

        // node id should never be empty
        if (!fields.containsKey("node_id")) {
            addThisNode();
        }

        // also the timestamp should never be empty
        if (!fields.containsKey("timestamp")) {
            fields.put("timestamp", Tools.getISO8601String(Tools.iso8601()));
        }

        // Write only if there is no such warning yet.
        if (!isFirst(core, this.type)) {
            return false;
        }
        try {
            this.save();
        } catch(ValidationException e) {
            // We have no validations, but just in case somebody adds some...
            LOG.error("Validating user warning failed.", e);
            return false;
        }

        return true;
    }

    public static boolean fixed(Core core, Type type) {
        return fixed(core, type, null);
    }

    public static boolean fixed(Core core, Type type, Node node) {
        BasicDBObject qry = new BasicDBObject();
        qry.put("type", type.toString().toLowerCase());
        if (node != null)
            qry.put("node_id", node.getNodeId());
        return destroy(qry, core, COLLECTION).getN() > 0;
    }

    public boolean fixed() {
        BasicDBObject qry = new BasicDBObject();
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            qry.put(entry.getKey(), entry.getValue());
        }

        return destroy(qry, core, COLLECTION).getN() > 0;
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

    public DateTime getTimestamp() {
        return timestamp;
    }

    public Type getType() {
        return type;
    }

    public Severity getSeverity() {
        return severity;
    }

    public Node getNode() {
        return Node.byNodeId(core, this.node_id);
    }

    public String getNodeId() {
        return this.node_id;
    }

    public Notification addDetail(String key, Object value) {
        Map<String, Object> details;
        if (fields.get("details") == null)
            fields.put("details", new HashMap<String, Object>());

        details = (Map<String, Object>)fields.get("details");
        details.put(key, value);
        return this;
    }

    public Map<String, Object> asMap() {
        Map<String, Object> result = Maps.newHashMap(fields);
        result.remove("_id");

        return result;
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
