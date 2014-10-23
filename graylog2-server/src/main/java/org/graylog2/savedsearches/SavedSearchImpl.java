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

    public SavedSearchImpl(Map<String, Object> fields) {
        super(fields);
    }

    public SavedSearchImpl(ObjectId id, Map<String, Object> fields) {
        super(id, fields);
    }

    @Override
    public Map<String, Validator> getValidations() {
        return ImmutableMap.<String, Validator>builder()
                .put("title", new FilledStringValidator())
                .put("query", new MapValidator())
                .put("creator_user_id", new FilledStringValidator())
                .put("created_at", new DateValidator())
                .build();
    }

    @Override
    public Map<String, Validator> getEmbeddedValidations(String key) {
        return Collections.emptyMap();
    }

    public Map<String, Object> asMap() {
        return ImmutableMap.<String, Object>builder()
                .put("id", ((ObjectId) fields.get("_id")).toHexString())
                .put("title", fields.get("title"))
                .put("query", fields.get("query"))
                .put("created_at", (Tools.getISO8601String((DateTime) fields.get("created_at"))))
                .put("creator_user_id", fields.get("creator_user_id"))
                .build();
    }
}
