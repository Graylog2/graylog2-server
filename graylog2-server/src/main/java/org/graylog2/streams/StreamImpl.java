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

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.bson.types.ObjectId;
import org.graylog2.database.CollectionName;
import org.graylog2.database.PersistedImpl;
import org.graylog2.database.validators.DateValidator;
import org.graylog2.database.validators.FilledStringValidator;
import org.graylog2.database.validators.MapValidator;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.database.validators.Validator;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Representing a single stream from the streams collection. Also provides method
 * to get all streams of this collection.
 *
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@CollectionName("streams")
public class StreamImpl extends PersistedImpl implements Stream {

    private static final Logger LOG = LoggerFactory.getLogger(StreamImpl.class);
    public static final String EMBEDDED_ALERT_CONDITIONS = "alert_conditions";
    private final List<StreamRule> streamRules;
    private final Set<Output> outputs;

    public StreamImpl(Map<String, Object> fields) {
    	super(fields);
        this.streamRules = null;
        this.outputs = null;
    }

    protected StreamImpl(ObjectId id, Map<String, Object> fields) {
    	super(id, fields);
        this.streamRules = null;
        this.outputs = null;
    }

    public StreamImpl(ObjectId id, Map<String, Object> fields, List<StreamRule> streamRules, Set<Output> outputs) {
        super(id, fields);

        this.streamRules = streamRules;
        this.outputs = outputs;
    }

    @Override
    public String toString() {
        return this.id.toString() + ": \"" + this.getTitle() + "\"";
    }

    @Override
    public List<StreamRule> getStreamRules() {
        return this.streamRules;
    }

    @Override
    public Set<Output> getOutputs() {
        return this.outputs;
    }

    @Override
	public String getTitle() {
		return (String) fields.get("title");
	}

    @Override
    public String getDescription() {
        return (String) fields.get("description");
    }

    @Override
    public void setTitle(String title) {
        fields.put("title", title);
    }

    @Override
    public void setDescription(String description) {
        fields.put("description", description);
    }

    @Override
    public Boolean getDisabled() {
        return (Boolean) fields.get("disabled");
    }

    @Override
    public void setDisabled(Boolean disabled) {
        fields.put("disabled", disabled);
    }

    public Boolean isPaused() {
        Boolean disabled = getDisabled();
        return (disabled != null && disabled);
    }
	
	public Map<String, Object> asMap(List<StreamRule> streamRules) {
        Map<String, Object> result = asMap();

        List<Map<String, Object>> streamRulesMap = Lists.newArrayList();

        for (StreamRule streamRule : streamRules) {
            streamRulesMap.add(streamRule.asMap());
        }

        result.put("rules", streamRulesMap);

		return result;
	}

    @JsonValue
    public Map<String, Object> asMap() {
        // We work on the result a bit to allow correct JSON serializing.
        Map<String, Object> result = Maps.newHashMap(fields);
        result.remove("_id");
        result.put("id", ((ObjectId) fields.get("_id")).toStringMongod());
        result.remove("created_at");
        result.put("created_at", (Tools.getISO8601String((DateTime) fields.get("created_at"))));
        result.put("rules", streamRules);
        result.put("outputs", outputs);
        return result;
    }

    public Map<String, Validator> getValidations() {
        return new HashMap<String, Validator>() {{
            put("title", new FilledStringValidator());
            put("creator_user_id", new FilledStringValidator());
            put("created_at", new DateValidator());
        }};
    }

    @Override
    public Map<String, Validator> getEmbeddedValidations(String key) {
       if(key.equals(EMBEDDED_ALERT_CONDITIONS)) {
            return new HashMap<String, Validator>() {{
                put("id", new FilledStringValidator());
                put("parameters", new MapValidator());
            }};
        }

        return Maps.newHashMap();
    }

    public Map<String, List<String>> getAlertReceivers() {
        if (!fields.containsKey("alert_receivers")) {
            return Maps.newHashMap();
        }

        return (Map<String, List<String>>) fields.get("alert_receivers");
    }

}