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

package org.graylog2.streams;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.alerts.AbstractAlertCondition;
import org.graylog2.alerts.AlertService;
import org.graylog2.alerts.AlertServiceImpl;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.PersistedServiceImpl;
import org.graylog2.database.ValidationException;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.database.EmbeddedPersistable;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StreamServiceImpl extends PersistedServiceImpl implements StreamService {
    private static final Logger LOG = LoggerFactory.getLogger(StreamServiceImpl.class);
    private final StreamRuleService streamRuleService;
    private final AlertService alertService;

    public StreamServiceImpl(MongoConnection mongoConnection) {
        this(mongoConnection,
                new StreamRuleServiceImpl(mongoConnection),
                new AlertServiceImpl(mongoConnection));
    }

    @Inject
    public StreamServiceImpl(MongoConnection mongoConnection, StreamRuleService streamRuleService, AlertService alertService) {
        super(mongoConnection);
        this.streamRuleService = streamRuleService;
        this.alertService = alertService;
    }

    @SuppressWarnings("unchecked")
    public Stream load(ObjectId id) throws NotFoundException {
        DBObject o = get(StreamImpl.class, id);

        if (o == null) {
            throw new NotFoundException();
        }

        return new StreamImpl((ObjectId) o.get("_id"), o.toMap());
    }

    public Stream load(String id) throws NotFoundException {
        return load(new ObjectId(id));
    }

    public List<Stream> loadAllEnabled() {
        return loadAllEnabled(new HashMap<String, Object>());
    }

    @SuppressWarnings("unchecked")
    public List<Stream> loadAllEnabled(Map<String, Object> additionalQueryOpts) {
        additionalQueryOpts.put("disabled", new BasicDBObject("$ne", true));

        return loadAll(additionalQueryOpts);
    }

    public List<Stream> loadAll() {
        return loadAll(new HashMap<String, Object>());
    }

    @SuppressWarnings("unchecked")
    public List<Stream> loadAll(Map<String, Object> additionalQueryOpts) {
        List<Stream> streams = Lists.newArrayList();

        DBObject query = new BasicDBObject();

        // putAll() is not working with BasicDBObject.
        for (Map.Entry<String, Object> o : additionalQueryOpts.entrySet()) {
            query.put(o.getKey(), o.getValue());
        }

        List<DBObject> results = query(StreamImpl.class, query);
        for (DBObject o : results) {
            streams.add(new StreamImpl((ObjectId) o.get("_id"), o.toMap()));
        }

        return streams;
    }

    public List<Stream> loadAllWithConfiguredAlertConditions() {
        Map<String, Object> queryOpts = new HashMap<String, Object>() {{
            // Explanation: alert_conditions.1 is the first Array element.
            put(StreamImpl.EMBEDDED_ALERT_CONDITIONS, new BasicDBObject("$ne", new ArrayList<Object>()));
        }};

        return loadAll(queryOpts);
    }

    public void destroy(Stream stream) throws NotFoundException {
        for (StreamRule streamRule : streamRuleService.loadForStream(stream)) {
            //super.destroy(streamRule);
        }
        super.destroy(stream);
    }

    public void update(Stream stream, String title, String description) throws ValidationException {
        if (title != null) {
            stream.getFields().put("title", title);
        }

        if (description != null) {
            stream.getFields().put("description", description);
        }

        save(stream);
    }

    @Override
    public void pause(Stream stream) {
        try {
            stream.setDisabled(true);
            save(stream);
        } catch (ValidationException e) {
            LOG.error("Caught exception while saving object: ", e);
        }
    }

    @Override
    public void resume(Stream stream) {
        try {
            stream.setDisabled(false);
            save(stream);
        } catch (ValidationException e) {
            LOG.error("Caught exception while saving object: ", e);
        }
    }

    @Override
    public List<StreamRule> getStreamRules(Stream stream) throws NotFoundException {
        return streamRuleService.loadForStream(stream);
    }

    public List<AlertCondition> getAlertConditions(Stream stream) {
        List<AlertCondition> conditions = Lists.newArrayList();

        if (stream.getFields().containsKey(StreamImpl.EMBEDDED_ALERT_CONDITIONS)) {
            for (BasicDBObject conditionFields : (List<BasicDBObject>) stream.getFields().get(StreamImpl.EMBEDDED_ALERT_CONDITIONS)) {
                try {
                    conditions.add(alertService.fromPersisted(conditionFields, stream));
                } catch (AbstractAlertCondition.NoSuchAlertConditionTypeException e) {
                    LOG.error("Skipping unknown alert condition type.", e);
                    continue;
                } catch (Exception e) {
                    LOG.error("Skipping alert condition.", e);
                    continue;
                }
            }
        }

        return conditions;
    }

    @Override
    public AlertCondition getAlertCondition(Stream stream, String conditionId) throws NotFoundException {
        if (stream.getFields().containsKey(StreamImpl.EMBEDDED_ALERT_CONDITIONS)) {
            for (BasicDBObject conditionFields : (List<BasicDBObject>) stream.getFields().get(StreamImpl.EMBEDDED_ALERT_CONDITIONS)) {
                try {
                    if (conditionFields.get("id").equals(conditionId)) {
                        return alertService.fromPersisted(conditionFields, stream);
                    }
                } catch (AbstractAlertCondition.NoSuchAlertConditionTypeException e) {
                    LOG.error("Skipping unknown alert condition type.", e);
                    continue;
                } catch (Exception e) {
                    LOG.error("Skipping alert condition.", e);
                    continue;
                }
            }
        }

        throw new org.graylog2.database.NotFoundException();
    }

    public void addAlertCondition(Stream stream, AlertCondition condition) throws ValidationException {
        embed(stream, StreamImpl.EMBEDDED_ALERT_CONDITIONS, (EmbeddedPersistable)condition);
    }

    @Override
    public void updateAlertCondition(Stream stream, AlertCondition condition) throws ValidationException {
        removeAlertCondition(stream, condition.getId());
        addAlertCondition(stream, condition);
    }

    public void removeAlertCondition(Stream stream, String conditionId) {
        removeEmbedded(stream, StreamImpl.EMBEDDED_ALERT_CONDITIONS, conditionId);
    }

    public void addAlertReceiver(Stream stream, String type, String name) {
        collection(stream).update(
                new BasicDBObject("_id", new ObjectId(stream.getId())),
                new BasicDBObject("$push", new BasicDBObject("alert_receivers." + type, name))
        );
    }

    public void removeAlertReceiver(Stream stream, String type, String name) {
        collection(stream).update(
                new BasicDBObject("_id", new ObjectId(stream.getId())),
                new BasicDBObject("$pull", new BasicDBObject("alert_receivers." + type, name))
        );
    }
}