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
package org.graylog2.dashboards;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.bson.types.ObjectId;
import org.graylog2.dashboards.widgets.DashboardWidget;
import org.graylog2.database.CollectionName;
import org.graylog2.database.PersistedImpl;
import org.graylog2.database.validators.DateValidator;
import org.graylog2.database.validators.FilledStringValidator;
import org.graylog2.database.validators.OptionalStringValidator;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.database.validators.Validator;
import org.joda.time.DateTime;

import java.util.Collections;
import java.util.Map;

@CollectionName("dashboards")
public class DashboardImpl extends PersistedImpl implements Dashboard {
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_CONTENT_PACK = "content_pack";
    public static final String FIELD_CREATOR_USER_ID = "creator_user_id";
    public static final String FIELD_CREATED_AT = "created_at";
    public static final String EMBEDDED_WIDGETS = "widgets";
    public static final String EMBEDDED_POSITIONS = "positions";

    private Map<String, DashboardWidget> widgets = Maps.newHashMap();

    public DashboardImpl(Map<String, Object> fields) {
        super(fields);
    }

    protected DashboardImpl(ObjectId id, Map<String, Object> fields) {
        super(id, fields);
    }

    @Override
    public String getTitle() {
        return (String) fields.get(FIELD_TITLE);
    }

    @Override
    public void setTitle(String title) {
        this.fields.put(FIELD_TITLE, title);
    }

    @Override
    public String getDescription() {
        return (String) fields.get(FIELD_DESCRIPTION);
    }

    @Override
    public void setDescription(String description) {
        this.fields.put(FIELD_DESCRIPTION, description);
    }

    @Override
    public String getContentPack() {
        return (String) fields.get(FIELD_CONTENT_PACK);
    }

    @Override
    public void setContentPack(String contentPack) {
        this.fields.put(FIELD_CONTENT_PACK, contentPack);
    }

    @Override
    public void addPersistedWidget(DashboardWidget widget) {
        widgets.put(widget.getId(), widget);
    }

    @Override
    public DashboardWidget getWidget(String widgetId) {
        return widgets.get(widgetId);
    }

    @Override
    public DashboardWidget addWidget(DashboardWidget widget) {
        return widgets.put(widget.getId(), widget);
    }

    @Override
    public DashboardWidget removeWidget(DashboardWidget widget) {
        return widgets.remove(widget.getId());
    }

    @Override
    public Map<String, DashboardWidget> getWidgets() {
        return ImmutableMap.copyOf(widgets);
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
        result.remove("_id");
        result.put("id", ((ObjectId) fields.get("_id")).toHexString());
        result.remove(FIELD_CREATED_AT);
        result.put(FIELD_CREATED_AT, (Tools.getISO8601String((DateTime) fields.get(FIELD_CREATED_AT))));

        if (!result.containsKey(EMBEDDED_WIDGETS)) {
            result.put(EMBEDDED_WIDGETS, Collections.emptyList());
        }

        if (!result.containsKey(EMBEDDED_POSITIONS)) {
            result.put(EMBEDDED_POSITIONS, Collections.emptyMap());
        }

        return result;
    }

}