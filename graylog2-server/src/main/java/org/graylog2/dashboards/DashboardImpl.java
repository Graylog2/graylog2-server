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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.bson.types.ObjectId;
import org.graylog2.dashboards.widgets.DashboardWidget;
import org.graylog2.database.CollectionName;
import org.graylog2.database.PersistedImpl;
import org.graylog2.database.validators.DateValidator;
import org.graylog2.database.validators.FilledStringValidator;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.database.validators.Validator;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@CollectionName("dashboards")
public class DashboardImpl extends PersistedImpl implements Dashboard {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardImpl.class);

    public static final String EMBEDDED_WIDGETS = "widgets";
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";

    private Map<String, DashboardWidget> widgets = Maps.newHashMap();

    public DashboardImpl(Map<String, Object> fields) {
        super(fields);
    }

    protected DashboardImpl(ObjectId id, Map<String, Object> fields) {
        super(id, fields);
    }

    @Override
    public String getTitle() {
        return (String) fields.get(TITLE);
    }


    @Override
    public void setTitle(String title) {
        this.fields.put(TITLE, title);
    }

    @Override
    public String getDescription() {
        return (String) fields.get(DESCRIPTION);
    }

    @Override
    public void setDescription(String description) {
        this.fields.put(DESCRIPTION, description);
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
    public Map<String, Validator> getValidations() {
        return new HashMap<String, Validator>() {{
            put(TITLE, new FilledStringValidator());
            put(DESCRIPTION, new FilledStringValidator());
            put("creator_user_id", new FilledStringValidator());
            put("created_at", new DateValidator());
        }};
    }

    @Override
    public Map<String, Validator> getEmbeddedValidations(String key) {
        return Maps.newHashMap();
    }

    @Override
    public Map<String, Object> asMap() {
        // We work on the result a bit to allow correct JSON serializing.
        Map<String, Object> result = Maps.newHashMap(fields);

        // TODO this sucks and should be done somewhere globally.
        result.remove("_id");
        result.put("id", ((ObjectId) fields.get("_id")).toStringMongod());
        result.remove("created_at");
        result.put("created_at", (Tools.getISO8601String((DateTime) fields.get("created_at"))));

        if (!result.containsKey("widgets")) {
            result.put("widgets", Lists.newArrayList());
        }

        if (!result.containsKey("positions")) {
            result.put("positions", Maps.newHashMap());
        }

        return result;
    }

}
