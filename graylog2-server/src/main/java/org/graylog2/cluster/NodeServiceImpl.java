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
package org.graylog2.cluster;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.Configuration;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PersistedServiceImpl;
import org.graylog2.database.ValidationException;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.system.NodeId;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class NodeServiceImpl extends PersistedServiceImpl implements NodeService {
    private final long pingTimeout;

    @Inject
    public NodeServiceImpl(final MongoConnection mongoConnection, final Configuration configuration) {
        super(mongoConnection);
        this.pingTimeout = TimeUnit.MILLISECONDS.toSeconds(configuration.getStaleMasterTimeout());
    }

    @Override
    public String registerServer(String nodeId, boolean isMaster, URI restTransportUri) {
        Map<String, Object> fields = Maps.newHashMap();
        fields.put("last_seen", Tools.getUTCTimestamp());
        fields.put("node_id", nodeId);
        fields.put("type", Node.Type.SERVER.toString());
        fields.put("is_master", isMaster);
        fields.put("transport_address", restTransportUri.toString());

        try {
            return save(new NodeImpl(fields));
        } catch (ValidationException e) {
            throw new RuntimeException("Validation failed.", e);
        }
    }

    @Override
    public String registerRadio(String nodeId, String restTransportUri) {
        Map<String, Object> fields = Maps.newHashMap();
        fields.put("last_seen", Tools.getUTCTimestamp());
        fields.put("node_id", nodeId);
        fields.put("type", Node.Type.RADIO.toString());
        fields.put("transport_address", restTransportUri);

        try {
            return save(new NodeImpl(fields));
        } catch (ValidationException e) {
            throw new RuntimeException("Validation failed.", e);
        }
    }

    @Override
    public Node byNodeId(String nodeId) throws NodeNotFoundException {
        DBObject query = new BasicDBObject("node_id", nodeId);
        DBObject o = findOne(NodeImpl.class, query);

        if (o == null || !o.containsField("node_id")) {
            throw new NodeNotFoundException("Unable to find node " + nodeId);
        }

        return new NodeImpl((ObjectId) o.get("_id"), o.toMap());
    }

    @Override
    public Node byNodeId(NodeId nodeId) throws NodeNotFoundException {
        return byNodeId(nodeId.toString());
    }

    @Override
    public Map<String, Node> allActive(Node.Type type) {
        Map<String, Node> nodes = Maps.newHashMap();

        BasicDBObject query = new BasicDBObject();
        query.put("last_seen", new BasicDBObject("$gte", Tools.getUTCTimestamp() - pingTimeout));
        query.put("type", type.toString());

        for (DBObject obj : query(NodeImpl.class, query)) {
            Node node = new NodeImpl((ObjectId) obj.get("_id"), obj.toMap());
            String nodeId = (String) obj.get("node_id");

            nodes.put(nodeId, node);
        }

        return nodes;
    }

    @Override
    public Map<String, Node> allActive() {
        Map<String, Node> nodes = Maps.newHashMap();

        for (Node.Type type : Node.Type.values())
            nodes.putAll(allActive(type));

        return nodes;
    }

    @Override
    public void dropOutdated() {
        BasicDBObject query = new BasicDBObject();
        query.put("last_seen", new BasicDBObject("$lt", Tools.getUTCTimestamp() - pingTimeout));

        destroyAll(NodeImpl.class, query);
    }

    /**
     * Mark this node as alive and probably update some settings that may have changed since last server boot.
     *
     * @param isMaster
     * @param restTransportAddress
     */
    @Override
    public void markAsAlive(Node node, boolean isMaster, String restTransportAddress) {
        node.getFields().put("last_seen", Tools.getUTCTimestamp());
        node.getFields().put("is_master", isMaster);
        node.getFields().put("transport_address", restTransportAddress);
        try {
            save(node);
        } catch (ValidationException e) {
            throw new RuntimeException("Validation failed.", e);
        }
    }

    @Override
    public void markAsAlive(Node node, boolean isMaster, URI restTransportAddress) {
        markAsAlive(node, isMaster, restTransportAddress.toString());
    }

    @Override
    public boolean isOnlyMaster(NodeId nodeId) {
        BasicDBObject query = new BasicDBObject();
        query.put("type", Node.Type.SERVER.toString());
        query.put("last_seen", new BasicDBObject("$gte", Tools.getUTCTimestamp() - pingTimeout));
        query.put("node_id", new BasicDBObject("$ne", nodeId.toString()));
        query.put("is_master", true);

        return query(NodeImpl.class, query).size() == 0;
    }

    @Override
    public boolean isAnyMasterPresent() {
        BasicDBObject query = new BasicDBObject();
        query.put("type", Node.Type.SERVER.toString());
        query.put("last_seen", new BasicDBObject("$gte", Tools.getUTCTimestamp() - pingTimeout));
        query.put("is_master", true);

        return query(NodeImpl.class, query).size() > 0;
    }
}