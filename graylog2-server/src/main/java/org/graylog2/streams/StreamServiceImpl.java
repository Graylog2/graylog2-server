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
package org.graylog2.streams;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.alerts.AbstractAlertCondition;
import org.graylog2.alerts.AlertService;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.PersistedServiceImpl;
import org.graylog2.database.ValidationException;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.database.EmbeddedPersistable;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.rest.resources.streams.requests.CreateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StreamServiceImpl extends PersistedServiceImpl implements StreamService {
    private static final Logger LOG = LoggerFactory.getLogger(StreamServiceImpl.class);
    private final StreamRuleService streamRuleService;
    private final AlertService alertService;
    private final OutputService outputService;
    private final NotificationService notificationService;

    @Inject
    public StreamServiceImpl(MongoConnection mongoConnection,
                             StreamRuleService streamRuleService,
                             AlertService alertService,
                             OutputService outputService,
                             NotificationService notificationService) {
        super(mongoConnection);
        this.streamRuleService = streamRuleService;
        this.alertService = alertService;
        this.outputService = outputService;
        this.notificationService = notificationService;
    }

    @SuppressWarnings("unchecked")
    public Stream load(ObjectId id) throws NotFoundException {
        DBObject o = get(StreamImpl.class, id);

        if (o == null) {
            throw new NotFoundException("Stream <" + id + "> not found!");
        }

        List<StreamRule> streamRules = streamRuleService.loadForStreamId(id.toHexString());

        Set<Output> outputs = loadOutputsForRawStream(o);

        return new StreamImpl((ObjectId) o.get("_id"), o.toMap(), streamRules, outputs);
    }

    @Override
    public Stream create(Map<String, Object> fields) {
        return new StreamImpl(fields);
    }

    @Override
    public Stream create(CreateRequest cr, String userId) {
        Map<String, Object> streamData = Maps.newHashMap();
        streamData.put(StreamImpl.FIELD_TITLE, cr.title);
        streamData.put(StreamImpl.FIELD_DESCRIPTION, cr.description);
        streamData.put(StreamImpl.FIELD_CREATOR_USER_ID, userId);
        streamData.put(StreamImpl.FIELD_CREATED_AT, Tools.iso8601());
        streamData.put(StreamImpl.FIELD_CONTENT_PACK, cr.contentPack);

        return create(streamData);
    }

    public Stream load(String id) throws NotFoundException {
        try {
            return load(new ObjectId(id));
        } catch (IllegalArgumentException e) {
            throw new NotFoundException("Stream <" + id + "> not found!");
        }
    }

    public List<Stream> loadAllEnabled() {
        return loadAllEnabled(new HashMap<String, Object>());
    }

    @SuppressWarnings("unchecked")
    public List<Stream> loadAllEnabled(Map<String, Object> additionalQueryOpts) {
        additionalQueryOpts.put(StreamImpl.FIELD_DISABLED, false);

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
            String id = o.get("_id").toString();
            List<StreamRule> streamRules = null;
            try {
                streamRules = streamRuleService.loadForStreamId(id);
            } catch (NotFoundException e) {
                LOG.info("Exception while loading stream rules: " + e);
            }

            final Set<Output> outputs = loadOutputsForRawStream(o);

            streams.add(new StreamImpl((ObjectId) o.get("_id"), o.toMap(), streamRules, outputs));
        }

        return streams;
    }

    public List<Stream> loadAllWithConfiguredAlertConditions() {
        // Explanation: alert_conditions.1 is the first Array element.
        Map<String, Object> queryOpts = Collections.<String, Object>singletonMap(
                StreamImpl.EMBEDDED_ALERT_CONDITIONS, new BasicDBObject("$ne", Collections.emptyList()));

        return loadAll(queryOpts);
    }

    protected Set<Output> loadOutputsForRawStream(DBObject stream) {
        List<ObjectId> outputIds = (List<ObjectId>) stream.get(StreamImpl.FIELD_OUTPUTS);

        Set<Output> result = new HashSet<>();
        if (outputIds != null)
            for (ObjectId outputId : outputIds)
                try {
                    result.add(outputService.load(outputId.toHexString()));
                } catch (NotFoundException e) {
                    LOG.warn("Non-existing output <{}> referenced from stream <{}>!", outputId.toHexString(), stream.get("_id"));
                }

        return result;

    }

    public void destroy(Stream stream) throws NotFoundException {
        for (StreamRule streamRule : streamRuleService.loadForStream(stream)) {
            super.destroy(streamRule);
        }
        for (Notification notification : notificationService.all()) {
            Object rawValue = notification.getDetail("stream_id");
            if (rawValue != null && rawValue.toString().equals(stream.getId())) {
                LOG.debug("Removing notification that references stream: {}", notification);
                notificationService.destroy(notification);
            }
        }
        super.destroy(stream);
    }

    public void update(Stream stream, String title, String description) throws ValidationException {
        if (title != null) {
            stream.getFields().put(StreamImpl.FIELD_TITLE, title);
        }

        if (description != null) {
            stream.getFields().put(StreamImpl.FIELD_DESCRIPTION, description);
        }

        save(stream);
    }

    @Override
    public void pause(Stream stream) throws ValidationException {
        stream.setDisabled(true);
        save(stream);
    }

    @Override
    public void resume(Stream stream) throws ValidationException {
        stream.setDisabled(false);
        save(stream);
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
        embed(stream, StreamImpl.EMBEDDED_ALERT_CONDITIONS, (EmbeddedPersistable) condition);
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

    @Override
    public void addOutput(Stream stream, Output output) {
        collection(stream).update(
                new BasicDBObject("_id", new ObjectId(stream.getId())),
                new BasicDBObject("$addToSet", new BasicDBObject(StreamImpl.FIELD_OUTPUTS, new ObjectId(output.getId())))
        );
    }

    @Override
    public void removeOutput(Stream stream, Output output) {
        collection(stream).update(
                new BasicDBObject("_id", new ObjectId(stream.getId())),
                new BasicDBObject("$pull", new BasicDBObject(StreamImpl.FIELD_OUTPUTS, new ObjectId(output.getId())))
        );
    }

    public void removeOutputFromAllStreams(Output output) {
        ObjectId outputId = new ObjectId(output.getId());
        DBObject match = new BasicDBObject(StreamImpl.FIELD_OUTPUTS, outputId);
        DBObject modify = new BasicDBObject("$pull", new BasicDBObject(StreamImpl.FIELD_OUTPUTS, outputId));

        collection(StreamImpl.class).update(
                match,
                modify
        );
    }
}
