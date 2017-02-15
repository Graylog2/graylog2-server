/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.savedsearches;

import com.google.common.collect.ImmutableMap;
import org.bson.types.ObjectId;
import org.graylog2.database.CollectionName;
import org.graylog2.database.PersistedImpl;
import org.graylog2.database.validators.DateValidator;
import org.graylog2.database.validators.FilledStringValidator;
import org.graylog2.database.validators.MapValidator;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.database.validators.Validator;
import org.joda.time.DateTime;

import java.util.Collections;
import java.util.Map;

@CollectionName("saved_searches")
public class SavedSearchImpl extends PersistedImpl implements SavedSearch {
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_QUERY = "query";
    public static final String FIELD_CREATED_AT = "created_at";
    public static final String FIELD_CREATOR_USER_ID = "creator_user_id";

    public SavedSearchImpl(Map<String, Object> fields) {
        super(fields);
    }

    public SavedSearchImpl(ObjectId id, Map<String, Object> fields) {
        super(id, fields);
    }

    @Override
    public Map<String, Validator> getValidations() {
        return ImmutableMap.<String, Validator>builder()
                .put(FIELD_TITLE, new FilledStringValidator())
                .put(FIELD_QUERY, new MapValidator())
                .put(FIELD_CREATOR_USER_ID, new FilledStringValidator())
                .put(FIELD_CREATED_AT, new DateValidator())
                .build();
    }

    @Override
    public Map<String, Validator> getEmbeddedValidations(String key) {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Object> asMap() {
        return ImmutableMap.<String, Object>builder()
                .put("id", ((ObjectId) fields.get("_id")).toHexString())
                .put(FIELD_TITLE, fields.get(FIELD_TITLE))
                .put(FIELD_QUERY, fields.get(FIELD_QUERY))
                .put(FIELD_CREATED_AT, Tools.getISO8601String((DateTime) fields.get(FIELD_CREATED_AT)))
                .put(FIELD_CREATOR_USER_ID, fields.get(FIELD_CREATOR_USER_ID))
                .build();
    }

    @Override
    public String getTitle() {
        return (String) fields.get(FIELD_TITLE);
    }
}
