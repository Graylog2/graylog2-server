/**
 * Copyright 2011, 2012, 2013 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2.streams;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.Core;
import org.graylog2.alerts.AlertCondition;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.Persisted;
import org.graylog2.database.ValidationException;
import org.graylog2.database.validators.DateValidator;
import org.graylog2.database.validators.FilledStringValidator;
import org.graylog2.database.validators.MapValidator;
import org.graylog2.database.validators.Validator;
import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.alarms.AlarmReceiver;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.rest.resources.streams.requests.CreateRequest;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Representing a single stream from the streams collection. Also provides method
 * to get all streams of this collection.
 *
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class StreamImpl extends Persisted implements Stream {

    private static final Logger LOG = LoggerFactory.getLogger(StreamImpl.class);

    private static final String COLLECTION = "streams";

    public static final String EMBEDDED_ALERT_CONDITIONS = "alert_conditions";

    public StreamImpl(Map<String, Object> fields, Core core) {
    	super(core, fields);
    }

    protected StreamImpl(ObjectId id, Map<String, Object> fields, Core core) {
    	super(core, id, fields);
    }
    
    @SuppressWarnings("unchecked")
	public static StreamImpl load(ObjectId id, Core core) throws NotFoundException {
    	BasicDBObject o = (BasicDBObject) get(id, core, COLLECTION);

    	if (o == null) {
    		throw new NotFoundException();
    	}

    	return new StreamImpl((ObjectId) o.get("_id"), o.toMap(), core);
    }
    
    public static List<Stream> loadAllEnabled(Core core) {
        return loadAllEnabled(core, new HashMap<String, Object>());
    }
    
    @SuppressWarnings("unchecked")
	public static List<Stream> loadAllEnabled(Core core, Map<String, Object> additionalQueryOpts) {
        additionalQueryOpts.put("disabled", new BasicDBObject("$ne", true));

    	return loadAll(core, additionalQueryOpts);
    }

    public static List<Stream> loadAll(Core core) {
        return loadAll(core, new HashMap<String, Object>());
    }

    @SuppressWarnings("unchecked")
    public static List<Stream> loadAll(Core core, Map<String, Object> additionalQueryOpts) {
        List<Stream> streams = Lists.newArrayList();

        DBObject query = new BasicDBObject();

        // putAll() is not working with BasicDBObject.
        for (Map.Entry<String, Object> o : additionalQueryOpts.entrySet()) {
            query.put(o.getKey(), o.getValue());
        }

        List<DBObject> results = query(query, core, COLLECTION);
        for (DBObject o : results) {
            streams.add(new StreamImpl((ObjectId) o.get("_id"), o.toMap(), core));
        }

        return streams;
    }

    public static List<Stream> loadAllWithConfiguredAlertConditions(Core core) {
        Map<String, Object> queryOpts = new HashMap<String, Object>() {{
            // Explanation: alert_conditions.1 is the first Array element.
            put(StreamImpl.EMBEDDED_ALERT_CONDITIONS, new BasicDBObject("$ne", new ArrayList<Object>()));
        }};

        return loadAll(core, queryOpts);
    }

    public void pause() {
        try {
            this.fields.put("disabled", true);
            this.save();
        } catch (ValidationException e) {
            LOG.error("Caught exception while saving object: ", e);
        }
    }

    public void resume() {
        try {
            this.fields.put("disabled", false);
            this.save();
        } catch (ValidationException e) {
            LOG.error("Caught exception while saving object: ", e);
        }
    }

    public Set<Map<String, String>> getOutputConfigurations(String className) {
    	return null;
    }
    
    public boolean hasConfiguredOutputs(String typeClass) {
    	return false;
    }

	@Override
	public List<StreamRule> getStreamRules() {
		// TODO Auto-generated method stub

        // TODO: PUT REGEX MATCHERS AT THE END
        // TODO: CONVERT TO INTS AS GOOD AS POSSIBLE IN CACHE

        List<StreamRule> streamRules;
        try {
            streamRules = StreamRuleImpl.findAllForStream(this.getId(), core);
        } catch (NotFoundException e) {
            streamRules = new ArrayList<StreamRule>();
        }

        return streamRules;
	}

    public List<AlertCondition> getAlertConditions() {
        List<AlertCondition> conditions = Lists.newArrayList();

        if (fields.containsKey(EMBEDDED_ALERT_CONDITIONS)) {
            for (BasicDBObject conditionFields : (List<BasicDBObject>) fields.get(EMBEDDED_ALERT_CONDITIONS)) {
                try {
                    conditions.add(AlertCondition.fromPersisted(conditionFields, this, core));
                } catch (AlertCondition.NoSuchAlertConditionTypeException e) {
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

    public void addAlertCondition(AlertCondition condition) throws ValidationException {
        embed(EMBEDDED_ALERT_CONDITIONS, condition);
    }

    public void removeAlertCondition(String conditionId) {
        removeEmbedded(EMBEDDED_ALERT_CONDITIONS, conditionId);
    }
	
    @Override
    public String toString() {
        return this.id.toString() + ": \"" + this.getTitle() + "\"";
    }

    @Override
    public String getCollectionName() {
        return COLLECTION;
    }

    @Override
	public String getTitle() {
		return (String) fields.get("title");
	}

    public Boolean isPaused() {
        Boolean disabled = (Boolean)this.fields.get("disabled");
        return (disabled != null && disabled);
    }
	
	public Map<String, Object> asMap() {
		// We work on the result a bit to allow correct JSON serializing.
		Map<String, Object> result = Maps.newHashMap(fields);
		result.remove("_id");
		result.put("id", ((ObjectId) fields.get("_id")).toStringMongod());
        result.remove("created_at");
        result.put("created_at", (Tools.getISO8601String((DateTime) fields.get("created_at"))));

        List<Map<String, Object>> streamRules = Lists.newArrayList();

        for (StreamRule streamRule : this.getStreamRules()) {
            streamRules.add(((StreamRuleImpl) streamRule).asMap());
        }

        result.put("rules", streamRules);

		return result;
	}

    protected Map<String, Validator> getValidations() {
        return new HashMap<String, Validator>() {{
            put("title", new FilledStringValidator());
            put("creator_user_id", new FilledStringValidator());
            put("created_at", new DateValidator());
        }};
    }

    @Override
    protected Map<String, Validator> getEmbeddedValidations(String key) {
       if(key.equals(EMBEDDED_ALERT_CONDITIONS)) {
            return new HashMap<String, Validator>() {{
                put("id", new FilledStringValidator());
                put("parameters", new MapValidator());
            }};
        }

        return Maps.newHashMap();
    }

    @Override
    public void destroy() {
        for (StreamRule streamRule : getStreamRules()) {
            ((StreamRuleImpl) streamRule).destroy();
        }
        super.destroy();
    }

    public void update(CreateRequest cr) throws ValidationException {
        if (cr.title != null) {
            this.fields.put("title", cr.title);
        }

        if (cr.description != null) {
            this.fields.put("description", cr.description);
        }

        save();
    }

    public Map<String, List<String>> getAlertReceivers() {
        if (!fields.containsKey("alert_receivers")) {
            return Maps.newHashMap();
        }

        return (Map<String, List<String>>) fields.get("alert_receivers");
    }

    public void addAlertReceiver(String type, String name) {
        collection().update(
                new BasicDBObject("_id", id),
                new BasicDBObject("$push", new BasicDBObject("alert_receivers." + type, name))
        );
    }

    public void removeAlertReceiver(String type, String name) {
        collection().update(
                new BasicDBObject("_id", id),
                new BasicDBObject("$pull", new BasicDBObject("alert_receivers." + type, name))
        );
    }

}