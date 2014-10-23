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

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.bson.types.ObjectId;
import org.graylog2.database.CollectionName;
import org.graylog2.database.PersistedImpl;
import org.graylog2.database.validators.DateValidator;
import org.graylog2.database.validators.FilledStringValidator;
import org.graylog2.database.validators.MapValidator;
import org.graylog2.database.validators.OptionalStringValidator;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.database.validators.Validator;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.joda.time.DateTime;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Representing a single stream from the streams collection. Also provides method
 * to get all streams of this collection.
 */
@CollectionName("streams")
public class StreamImpl extends PersistedImpl implements Stream {
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_RULES = "rules";
    public static final String FIELD_OUTPUTS = "outputs";
    public static final String FIELD_CONTENT_PACK = "content_pack";
    public static final String FIELD_ALERT_RECEIVERS = "alert_receivers";
    public static final String FIELD_DISABLED = "disabled";
    public static final String FIELD_CREATED_AT = "created_at";
    public static final String FIELD_CREATOR_USER_ID = "creator_user_id";
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
        return (String) fields.get(FIELD_TITLE);
    }

    @Override
    public String getDescription() {
        return (String) fields.get(FIELD_DESCRIPTION);
    }

    @Override
    public void setTitle(String title) {
        fields.put(FIELD_TITLE, title);
    }

    @Override
    public void setDescription(String description) {
        fields.put(FIELD_DESCRIPTION, description);
    }

    @Override
    public Boolean getDisabled() {
        return (Boolean) fields.get(FIELD_DISABLED);
    }

    @Override
    public void setDisabled(Boolean disabled) {
        fields.put(FIELD_DISABLED, disabled);
    }

    @Override
    public String getContentPack() {
        return (String) fields.get(FIELD_CONTENT_PACK);
    }

    @Override
    public void setContentPack(String contentPack) {
        fields.put(FIELD_CONTENT_PACK, contentPack);
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

        result.put(FIELD_RULES, streamRulesMap);

        return result;
    }

    @JsonValue
    public Map<String, Object> asMap() {
        // We work on the result a bit to allow correct JSON serializing.
        Map<String, Object> result = Maps.newHashMap(fields);
        result.remove("_id");
        result.put("id", ((ObjectId) fields.get("_id")).toHexString());
        result.remove(FIELD_CREATED_AT);
        result.put(FIELD_CREATED_AT, (Tools.getISO8601String((DateTime) fields.get(FIELD_CREATED_AT))));
        result.put(FIELD_RULES, streamRules);
        result.put(FIELD_OUTPUTS, outputs);
        return result;
    }

    public Map<String, Validator> getValidations() {
        return ImmutableMap.<String, Validator>builder()
                .put(FIELD_TITLE, new FilledStringValidator())
                .put(FIELD_CREATOR_USER_ID, new FilledStringValidator())
                .put(FIELD_CREATED_AT, new DateValidator())
                .put(FIELD_CONTENT_PACK, new OptionalStringValidator())
                .build();
    }

    @Override
    public Map<String, Validator> getEmbeddedValidations(String key) {
        if (key.equals(EMBEDDED_ALERT_CONDITIONS)) {
            return ImmutableMap.of(
                    "id", new FilledStringValidator(),
                    "parameters", new MapValidator());
        }

        return Collections.emptyMap();
    }

    public Map<String, List<String>> getAlertReceivers() {
        if (!fields.containsKey(FIELD_ALERT_RECEIVERS)) {
            return Collections.emptyMap();
        }

        return (Map<String, List<String>>) fields.get(FIELD_ALERT_RECEIVERS);
    }

}