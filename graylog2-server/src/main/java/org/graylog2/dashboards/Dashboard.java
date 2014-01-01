/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
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
package org.graylog2.dashboards;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.Core;
import org.graylog2.dashboards.widgets.DashboardWidget;
import org.graylog2.dashboards.widgets.InvalidWidgetConfigurationException;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.Persisted;
import org.graylog2.database.ValidationException;
import org.graylog2.database.validators.DateValidator;
import org.graylog2.database.validators.FilledStringValidator;
import org.graylog2.database.validators.Validator;
import org.graylog2.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.Tools;
import org.graylog2.rest.resources.dashboards.requests.WidgetPositionRequest;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Dashboard extends Persisted {

    private static final Logger LOG = LoggerFactory.getLogger(Dashboard.class);

    public static final String COLLECTION = "dashboards";

    public static final String EMBEDDED_WIDGETS = "widgets";

    private Map<String, DashboardWidget> widgets = Maps.newHashMap();

    public Dashboard(Map<String, Object> fields, Core core) {
        super(core, fields);
    }

    protected Dashboard(ObjectId id, Map<String, Object> fields, Core core) {
        super(core, id, fields);
    }

    @Override
    public String getCollectionName() {
        return COLLECTION;
    }

    public static Dashboard load(ObjectId id, Core core) throws NotFoundException {
        BasicDBObject o = (BasicDBObject) get(id, core, COLLECTION);

        if (o == null) {
            throw new NotFoundException();
        }

        return new Dashboard((ObjectId) o.get("_id"), o.toMap(), core);
    }

    public static List<Dashboard> all(Core core) {
        List<Dashboard> dashboards = Lists.newArrayList();

        List<DBObject> results = query(new BasicDBObject(), core, COLLECTION);
        for (DBObject o : results) {
            Map<String, Object> fields = o.toMap();
            Dashboard dashboard = new Dashboard((ObjectId) o.get("_id"), fields, core);

            // Add all widgets of this dashboard.
            if (fields.containsKey(EMBEDDED_WIDGETS)) {
                for (BasicDBObject widgetFields : (List<BasicDBObject>) fields.get(EMBEDDED_WIDGETS)) {
                    DashboardWidget widget = null;
                    try {
                        widget = DashboardWidget.fromPersisted(core, widgetFields);
                    } catch (DashboardWidget.NoSuchWidgetTypeException e) {
                        LOG.error("No such widget type: [{}] - Dashboard: [" + dashboard.getId() + "]", widgetFields.get("type"), e);
                        continue;
                    } catch (InvalidRangeParametersException e) {
                        LOG.error("Invalid range parameters of widget in dashboard: [{}]", dashboard.getId(), e);
                        continue;
                    } catch (InvalidWidgetConfigurationException e) {
                        LOG.error("Invalid configuration of widget in dashboard: [{}]", dashboard.getId(), e);
                        continue;
                    }
                    dashboard.addPersistedWidget(widget);
                }
            }


            dashboards.add(dashboard);
        }

        return dashboards;
    }

    public void setTitle(String title) {
        this.fields.put("title", title);
    }

    public void setDescription(String description) {
        this.fields.put("description", description);
    }

    public void addPersistedWidget(DashboardWidget widget) {
        widgets.put(widget.getId(), widget);
    }

    public void updateWidgetPositions(List<WidgetPositionRequest> positions) throws ValidationException {
        Map<String, Map<String, Object>> map = Maps.newHashMap();

        for (WidgetPositionRequest position : positions) {
            Map<String, Object> x = Maps.newHashMap();
            x.put("col", position.col);
            x.put("row", position.row);

            map.put(position.id, x);
        }

        fields.put("positions", map);

        save();
    }

    public void addWidget(DashboardWidget widget) throws ValidationException {
        embed(EMBEDDED_WIDGETS, widget);
        widgets.put(widget.getId(), widget);
    }

    public void removeWidget(String widgetId) {
        removeEmbedded(EMBEDDED_WIDGETS, widgetId);
        widgets.remove(widgetId);
    }

    public DashboardWidget getWidget(String widgetId) {
        return widgets.get(widgetId);
    }

    public void updateWidgetDescription(DashboardWidget widget, String newDescription) throws ValidationException {
        // Updating objects in arrays is a bit flaky in MongoDB. Let'S go the simple and stupid way until weh ave a proper DBA layer.
        widget.setDescription(newDescription);
        removeWidget(widget.getId());
        addWidget(widget);
    }

    public void updateWidgetCacheTime(DashboardWidget widget, int cacheTime) throws ValidationException {
        // Updating objects in arrays is a bit flaky in MongoDB. Let'S go the simple and stupid way until weh ave a proper DBA layer.
        widget.setCacheTime(cacheTime);
        removeWidget(widget.getId());
        addWidget(widget);
    }

    @Override
    protected Map<String, Validator> getValidations() {
        return new HashMap<String, Validator>() {{
            put("title", new FilledStringValidator());
            put("description", new FilledStringValidator());
            put("creator_user_id", new FilledStringValidator());
            put("created_at", new DateValidator());
        }};
    }

    @Override
    protected Map<String, Validator> getEmbeddedValidations(String key) {
        return Maps.newHashMap();
    }

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
            result.put("positions", Lists.newArrayList());
        }

        return result;
    }

}
