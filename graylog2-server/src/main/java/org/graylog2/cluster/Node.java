/**
 * Copyright2013 Lennart Koopmann <lennart@socketfeed.com>
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
package org.graylog2.cluster;

import com.google.common.collect.Maps;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.graylog2.Core;
import org.graylog2.database.Persisted;
import org.graylog2.database.ValidationException;
import org.graylog2.database.validators.Validator;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class Node extends Persisted {

    private static final Logger LOG = LoggerFactory.getLogger(Node.class);

    public static final int PING_TIMEOUT = 2;
    private static final String COLLECTION = "nodes";

    public enum Type {
        SERVER,
        RADIO
    }

    protected Node(Core core, Map<String, Object> fields) {
        super(core, fields);
    }

    protected Node(Core core, ObjectId id, Map<String, Object> fields) {
        super(core, id, fields);
    }

    public static void registerServer(Core core, boolean isMaster, URI restTransportUri) {
        Map<String, Object> fields = Maps.newHashMap();
        fields.put("last_seen", Tools.getUTCTimestamp());
        fields.put("node_id", core.getNodeId());
        fields.put("type", Type.SERVER.toString());
        fields.put("is_master", isMaster);
        fields.put("transport_address", restTransportUri.toString());

        try {
            new Node(core, fields).save();
        } catch (ValidationException e) {
            throw new RuntimeException("Validation failed.", e);
        }
    }

    public static void registerRadio(Core core, String nodeId, String restTransportUri) {
        Map<String, Object> fields = Maps.newHashMap();
        fields.put("last_seen", Tools.getUTCTimestamp());
        fields.put("node_id", nodeId);
        fields.put("type", Type.RADIO.toString());
        fields.put("transport_address", restTransportUri);

        try {
            new Node(core, fields).save();
        } catch (ValidationException e) {
            throw new RuntimeException("Validation failed.", e);
        }
    }

    public static Node thisNode(Core core) throws NodeNotFoundException {
        DBObject o = findOne(new BasicDBObject("node_id", core.getNodeId()), core, COLLECTION);

        if (o == null || !o.containsField("node_id")) {
            throw new NodeNotFoundException("Did not find our own node. This should never happen.");
        }

        return new Node(core, (ObjectId) o.get("_id"), o.toMap());
    }

    public static Node byNodeId(Core core, String nodeId) {
        DBObject query = new BasicDBObject("node_id", nodeId);
        DBObject o = findOne(query, core, COLLECTION);

        if (o == null) {
            return null;
        }

        return new Node(core, (ObjectId) o.get("_id"), o.toMap());
    }

    public static Map<String, Node> allActive(Core core, Type type) {
        Map<String, Node> nodes = Maps.newHashMap();

        BasicDBObject query = new BasicDBObject();
        query.put("last_seen", new BasicDBObject("$gte", Tools.getUTCTimestamp()-PING_TIMEOUT));
        query.put("type", type.toString());

        for(DBObject obj : query(query, core, COLLECTION)) {
            Node node = new Node(core, (ObjectId) obj.get("_id"), obj.toMap());
            String nodeId = (String) obj.get("node_id");

            nodes.put(nodeId, node);
        }

        return nodes;
    }

    public static void dropOutdated(Core core) {
        BasicDBObject query = new BasicDBObject();
        query.put("last_seen", new BasicDBObject("$lt", Tools.getUTCTimestamp()-PING_TIMEOUT));

        destroy(query, core, COLLECTION);
    }

    public boolean isOnlyMaster() {
        BasicDBObject query = new BasicDBObject();
        query.put("type", Type.SERVER.toString());
        query.put("last_seen", new BasicDBObject("$gte", Tools.getUTCTimestamp()-PING_TIMEOUT));
        query.put("node_id", new BasicDBObject("$ne", core.getNodeId()));
        query.put("is_master", true);

        return query(query, COLLECTION).size() == 0;
    }

    /**
     * Mark this node as alive and probably update some settings that may have changed since last server boot.
     *
     * @param isMaster
     * @param restTransportAddress
     */
    public void markAsAlive(boolean isMaster, String restTransportAddress) {
        fields.put("last_seen", Tools.getUTCTimestamp());
        fields.put("is_master", isMaster);
        fields.put("transport_address", restTransportAddress);
        try {
            save();
        } catch (ValidationException e) {
            throw new RuntimeException("Validation failed.", e);
        }
    }

    public void markAsAlive(boolean isMaster, URI restTransportAddress) {
        markAsAlive(isMaster, restTransportAddress.toString());
    }

    public String getNodeId() {
        return (String) fields.get("node_id");
    }

    public boolean isMaster() {
        return (Boolean) fields.get("is_master");
    }

    public String getTransportAddress() {
        return (String) fields.get("transport_address");
    }

    public DateTime getLastSeen() {
        return new DateTime(((Integer) fields.get("last_seen"))*1000L, DateTimeZone.UTC);
    }

    @Override
    public String getCollectionName() {
        return COLLECTION;
    }

    @Override
    protected Map<String, Validator> getValidations() {
        return Maps.newHashMap();
    }

    public String getShortNodeId() {
        return getNodeId().split("-")[0];
    }

    public Type getType() {
        if (!fields.containsKey("type")) {
            return Type.SERVER;
        }

        return Type.valueOf(fields.get("type").toString().toUpperCase());
    }

    @Override
    protected Map<String, Validator> getEmbeddedValidations(String key) {
        return Maps.newHashMap();
    }

}
