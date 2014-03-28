/*
 * Copyright 2012-2014 TORCH GmbH
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
 */

package org.graylog2.inputs;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.Core;
import org.graylog2.cluster.Node;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.PersistedServiceImpl;
import org.graylog2.database.ValidationException;
import org.graylog2.plugin.database.EmbeddedPersistable;
import org.graylog2.plugin.inputs.Extractor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InputServiceImpl extends PersistedServiceImpl implements InputService {
    @Inject
    public InputServiceImpl(MongoConnection mongoConnection) {
        super(mongoConnection);
    }

    @Override
    public List<Input> allOfThisNode(String nodeId) {
        List<Input> inputs = Lists.newArrayList();
        List<BasicDBObject> query = new ArrayList<BasicDBObject>();
        query.add(new BasicDBObject("node_id", nodeId));
        query.add(new BasicDBObject("global", true));
        List<DBObject> ownInputs = query(org.graylog2.inputs.InputImpl.class, new BasicDBObject("$or", query));
        for (DBObject o : ownInputs) {
            inputs.add(new org.graylog2.inputs.InputImpl((ObjectId) o.get("_id"), o.toMap()));
        }

        return inputs;
    }

    @Override
    public List<Input> allOfThisNode(Core core) {
        return allOfThisNode(core.getNodeId());
    }

    @Override
    public List<Input> allOfRadio(Node radio) {
        List<Input> inputs = Lists.newArrayList();
        List<BasicDBObject> query = Lists.newArrayList();
        query.add(new BasicDBObject("radio_id", radio.getNodeId()));
        query.add(new BasicDBObject("global", true));

        for (DBObject o : query(org.graylog2.inputs.InputImpl.class, new BasicDBObject("$or", query))) {
            inputs.add(new org.graylog2.inputs.InputImpl((ObjectId) o.get("_id"), o.toMap()));
        }

        return inputs;
    }

    @Override
    public Input find(String id) {
        DBObject o = get(org.graylog2.inputs.InputImpl.class, id);
        return new org.graylog2.inputs.InputImpl((ObjectId) o.get("_id"), o.toMap());
    }

    @Override
    public Input findForThisNodeOrGlobal(String nodeId, String id) throws NotFoundException {
        List<BasicDBObject> query = new ArrayList<BasicDBObject>();
        query.add(new BasicDBObject("_id", new ObjectId(id)));

        List<BasicDBObject> forThisNodeOrGlobal = new ArrayList<BasicDBObject>();
        forThisNodeOrGlobal.add(new BasicDBObject("node_id", nodeId));
        forThisNodeOrGlobal.add(new BasicDBObject("global", true));

        query.add(new BasicDBObject("$or", forThisNodeOrGlobal));

        DBObject o = findOne(InputImpl.class, new BasicDBObject("$and", query));

        return new InputImpl((ObjectId) o.get("_id"), o.toMap());
    }

    @Override
    public Input findForThisNode(String nodeId, String id) throws NotFoundException {
        List<BasicDBObject> query = new ArrayList<BasicDBObject>();
        query.add(new BasicDBObject("_id", new ObjectId(id)));

        List<BasicDBObject> forThisNode = new ArrayList<BasicDBObject>();
        forThisNode.add(new BasicDBObject("node_id", nodeId));
        forThisNode.add(new BasicDBObject("global", false));

        query.add(new BasicDBObject("$and", forThisNode));

        DBObject o = findOne(InputImpl.class, new BasicDBObject("$and", query));

        return new InputImpl((ObjectId) o.get("_id"), o.toMap());
    }

    @Override
    public void addExtractor(Input input, Extractor extractor) throws ValidationException {
        embed(input, InputImpl.EMBEDDED_EXTRACTORS, extractor);
    }

    @Override
    public void addStaticField(Input input, final String key, final String value) throws ValidationException {
        EmbeddedPersistable obj = new EmbeddedPersistable() {
            @Override
            public Map<String, Object> getPersistedFields() {
                return new HashMap<String, Object>() {{
                    put("key", key);
                    put("value", value);
                }};
            }
        };

        embed(input, InputImpl.EMBEDDED_STATIC_FIELDS, obj);
    }

    @Override
    public void removeExtractor(Input input, String extractorId) {
        removeEmbedded(input, InputImpl.EMBEDDED_EXTRACTORS, extractorId);
    }

    @Override
    public void removeStaticField(Input input, String key) {
        removeEmbedded(input, InputImpl.EMBEDDED_STATIC_FIELDS, key);
    }
}