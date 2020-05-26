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
package org.graylog2.dashboards;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.graylog2.dashboards.widgets.WidgetPosition;
import org.graylog2.database.CollectionName;
import org.graylog2.database.PersistedImpl;
import org.graylog2.database.validators.DateValidator;
import org.graylog2.database.validators.FilledStringValidator;
import org.graylog2.database.validators.OptionalStringValidator;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.database.validators.Validator;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Integer.parseInt;

@CollectionName("dashboards")
public class DashboardImpl extends PersistedImpl implements Dashboard {
    public static final String FIELD_ID = "_id";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_CONTENT_PACK = "content_pack";
    public static final String FIELD_CREATOR_USER_ID = "creator_user_id";
    public static final String FIELD_CREATED_AT = "created_at";
    public static final String EMBEDDED_WIDGETS = "widgets";
    public static final String EMBEDDED_POSITIONS = "positions";

    protected DashboardImpl(ObjectId id, Map<String, Object> fields) {
        super(id, fields);
    }

    @Override
    public String getTitle() {
        return (String) fields.get(FIELD_TITLE);
    }

    @Override
    public List<WidgetPosition> getPositions() {
        final BasicDBObject positions = (BasicDBObject) fields.get(DashboardImpl.EMBEDDED_POSITIONS);
        if (positions == null) {
            return Collections.emptyList();
        }

        final List<WidgetPosition> result = new ArrayList<>(positions.size());
        for ( String positionId : positions.keySet() ) {
            final BasicDBObject position = (BasicDBObject) positions.get(positionId);
            final int width = parseInt(position.getString("width", "1"));
            final int height = parseInt(position.getString("height", "1"));
            final int col = parseInt(position.getString("col", "1"));
            final int row = parseInt(position.getString("row","1"));
            final WidgetPosition widgetPosition = WidgetPosition.builder()
                    .id(positionId)
                    .width(width)
                    .height(height)
                    .col(col)
                    .row(row)
                    .build();
            result.add(widgetPosition);
        }
        return result;
    }

    @Override
    public void setPositions(List<WidgetPosition> widgetPositions) {
        checkNotNull(widgetPositions, "widgetPositions must be given");
        final Map<String, Map<String, Integer>> positions = new HashMap<>(widgetPositions.size());
        for (WidgetPosition widgetPosition : widgetPositions) {
            Map<String, Integer> position = new HashMap<>(4);
            position.put("width", widgetPosition.width());
            position.put("height", widgetPosition.height());
            position.put("col", widgetPosition.col());
            position.put("row", widgetPosition.row());
            positions.put(widgetPosition.id(), position);
        }
        Map<String, Object> fields = getFields();
        checkNotNull(fields, "No fields found!");
        fields.put(DashboardImpl.EMBEDDED_POSITIONS, positions);
    }

    @Override
    public Map<String, Validator> getValidations() {
        return ImmutableMap.<String, Validator>builder()
                .put(FIELD_TITLE, new FilledStringValidator())
                .put(FIELD_DESCRIPTION, new FilledStringValidator())
                .put(FIELD_CONTENT_PACK, new OptionalStringValidator())
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
        // We work on the result a bit to allow correct JSON serializing.
        Map<String, Object> result = Maps.newHashMap(fields);

        // TODO this sucks and should be done somewhere globally.
        result.remove(FIELD_ID);
        result.put("id", ((ObjectId) fields.get(FIELD_ID)).toHexString());
        result.remove(FIELD_CREATED_AT);
        result.put(FIELD_CREATED_AT, Tools.getISO8601String((DateTime) fields.get(FIELD_CREATED_AT)));

        if (!result.containsKey(EMBEDDED_WIDGETS)) {
            result.put(EMBEDDED_WIDGETS, Collections.emptyList());
        }

        if (!result.containsKey(EMBEDDED_POSITIONS)) {
            result.put(EMBEDDED_POSITIONS, Collections.emptyMap());
        }

        return result;
    }
}
