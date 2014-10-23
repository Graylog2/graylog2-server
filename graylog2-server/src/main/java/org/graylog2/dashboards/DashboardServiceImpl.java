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

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.dashboards.widgets.DashboardWidget;
import org.graylog2.dashboards.widgets.InvalidWidgetConfigurationException;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.PersistedServiceImpl;
import org.graylog2.database.ValidationException;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.rest.resources.dashboards.requests.WidgetPositionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class DashboardServiceImpl extends PersistedServiceImpl implements DashboardService {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardServiceImpl.class);
    private final MetricRegistry metricRegistry;
    private final Searches searches;

    @Inject
    public DashboardServiceImpl(MongoConnection mongoConnection,
                                MetricRegistry metricRegistry,
                                Searches searches) {
        super(mongoConnection);
        this.metricRegistry = metricRegistry;
        this.searches = searches;
    }

    @Override
    public Dashboard load(String id) throws NotFoundException {
        BasicDBObject o = (BasicDBObject) get(DashboardImpl.class, id);

        if (o == null) {
            throw new NotFoundException();
        }

        return new DashboardImpl((ObjectId) o.get("_id"), o.toMap());
    }

    @Override
    public List<Dashboard> all() {
        List<Dashboard> dashboards = Lists.newArrayList();

        List<DBObject> results = query(DashboardImpl.class, new BasicDBObject());
        for (DBObject o : results) {
            Map<String, Object> fields = o.toMap();
            Dashboard dashboard = new DashboardImpl((ObjectId) o.get("_id"), fields);

            // Add all widgets of this dashboard.
            if (fields.containsKey(DashboardImpl.EMBEDDED_WIDGETS)) {
                for (BasicDBObject widgetFields : (List<BasicDBObject>) fields.get(DashboardImpl.EMBEDDED_WIDGETS)) {
                    DashboardWidget widget = null;
                    try {
                        widget = DashboardWidget.fromPersisted(metricRegistry, searches, widgetFields);
                    } catch (DashboardWidget.NoSuchWidgetTypeException e) {
                        LOG.error("No such widget type: [" + widgetFields.get("type") + "] - Dashboard: [" + dashboard.getId() + "]", e);
                        continue;
                    } catch (InvalidRangeParametersException e) {
                        LOG.error("Invalid range parameters of widget in dashboard: [" + dashboard.getId() + "]", e);
                        continue;
                    } catch (InvalidWidgetConfigurationException e) {
                        LOG.error("Invalid configuration of widget in dashboard: [" + dashboard.getId() + "]", e);
                        continue;
                    }
                    dashboard.addPersistedWidget(widget);
                }
            }


            dashboards.add(dashboard);
        }

        return dashboards;
    }

    @Override
    public void updateWidgetPositions(Dashboard dashboard, List<WidgetPositionRequest> positions) throws ValidationException {
        Map<String, Map<String, Object>> map = Maps.newHashMap();

        for (WidgetPositionRequest position : positions) {
            Map<String, Object> x = Maps.newHashMap();
            x.put("col", position.col);
            x.put("row", position.row);

            map.put(position.id, x);
        }

        dashboard.getFields().put(DashboardImpl.EMBEDDED_POSITIONS, map);

        save(dashboard);
    }

    @Override
    public void addWidget(Dashboard dashboard, DashboardWidget widget) throws ValidationException {
        embed(dashboard, DashboardImpl.EMBEDDED_WIDGETS, widget);
        dashboard.addWidget(widget);
    }

    @Override
    public void removeWidget(Dashboard dashboard, DashboardWidget widget) {
        removeEmbedded(dashboard, DashboardImpl.EMBEDDED_WIDGETS, widget.getId());
        dashboard.removeWidget(widget);
    }

    @Override
    public void updateWidgetDescription(Dashboard dashboard, DashboardWidget widget, String newDescription) throws ValidationException {
        // Updating objects in arrays is a bit flaky in MongoDB. Let'S go the simple and stupid way until weh ave a proper DBA layer.
        widget.setDescription(newDescription);
        removeWidget(dashboard, widget);
        addWidget(dashboard, widget);
    }

    @Override
    public void updateWidgetCacheTime(Dashboard dashboard, DashboardWidget widget, int cacheTime) throws ValidationException {
        // Updating objects in arrays is a bit flaky in MongoDB. Let'S go the simple and stupid way until weh ave a proper DBA layer.
        widget.setCacheTime(cacheTime);
        removeWidget(dashboard, widget);
        addWidget(dashboard, widget);
    }
}