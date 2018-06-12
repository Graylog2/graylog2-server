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

import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.dashboards.events.DashboardDeletedEvent;
import org.graylog2.dashboards.widgets.DashboardWidget;
import org.graylog2.dashboards.widgets.DashboardWidgetCreator;
import org.graylog2.dashboards.widgets.InvalidWidgetConfigurationException;
import org.graylog2.dashboards.widgets.WidgetPosition;
import org.graylog2.dashboards.widgets.events.WidgetUpdatedEvent;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.PersistedServiceImpl;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.rest.models.dashboards.requests.WidgetPositionsRequest;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

public class DashboardServiceImpl extends PersistedServiceImpl implements DashboardService {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardServiceImpl.class);
    private final DashboardWidgetCreator dashboardWidgetCreator;

    private final ClusterEventBus clusterEventBus;
    private final EventBus serverEventBus;

    @Inject
    public DashboardServiceImpl(MongoConnection mongoConnection,
                                DashboardWidgetCreator dashboardWidgetCreator,
                                ClusterEventBus clusterEventBus,
                                EventBus serverEventBus) {
        super(mongoConnection);
        this.dashboardWidgetCreator = dashboardWidgetCreator;
        this.clusterEventBus = clusterEventBus;
        this.serverEventBus = serverEventBus;
    }

    @Override
    public Dashboard create(String title, String description, String creatorUserId, DateTime createdAt) {
        Map<String, Object> dashboardData = Maps.newHashMap();
        dashboardData.put(DashboardImpl.FIELD_TITLE, title);
        dashboardData.put(DashboardImpl.FIELD_DESCRIPTION, description);
        dashboardData.put(DashboardImpl.FIELD_CREATOR_USER_ID, creatorUserId);
        dashboardData.put(DashboardImpl.FIELD_CREATED_AT, createdAt);

        return new DashboardImpl(dashboardData);
    }

    private Dashboard create(ObjectId id, Map<String, Object> fields) {
        final Dashboard dashboard = new DashboardImpl(id, fields);
        // Add all widgets of this dashboard.
        if (fields.containsKey(DashboardImpl.EMBEDDED_WIDGETS)) {
            if (fields.get(DashboardImpl.EMBEDDED_WIDGETS) instanceof List) {
                for (BasicDBObject widgetFields : (List<BasicDBObject>) fields.get(DashboardImpl.EMBEDDED_WIDGETS)) {
                    try {
                        final DashboardWidget widget = dashboardWidgetCreator.fromPersisted(widgetFields);
                        dashboard.addPersistedWidget(widget);
                    } catch (DashboardWidget.NoSuchWidgetTypeException e) {
                        LOG.error("No such widget type: [" + widgetFields.get("type") + "] - Dashboard: [" + dashboard.getId() + "]", e);
                    } catch (InvalidRangeParametersException e) {
                        LOG.error("Invalid range parameters of widget in dashboard: [" + dashboard.getId() + "]", e);
                    } catch (InvalidWidgetConfigurationException e) {
                        LOG.error("Invalid configuration of widget in dashboard: [" + dashboard.getId() + "]", e);
                    }
                }
            }
        }

        return dashboard;
    }

    @Override
    public Dashboard load(String id) throws NotFoundException {
        final BasicDBObject o = (BasicDBObject) get(DashboardImpl.class, id);

        if (o == null) {
            throw new NotFoundException("Couldn't find dashboard with ID " + id);
        }

        return this.create((ObjectId) o.get(DashboardImpl.FIELD_ID), o.toMap());
    }

    @Override
    public List<Dashboard> all() {
        final List<DBObject> results = query(DashboardImpl.class, new BasicDBObject());

        final Stream<Dashboard> dashboardStream = results.stream()
                .map(o -> (Dashboard) new DashboardImpl((ObjectId) o.get(DashboardImpl.FIELD_ID), o.toMap()));
        return dashboardStream
                .collect(Collectors.toList());
    }

    @Override
    public Set<Dashboard> loadByIds(Collection<String> ids) {
        final Set<ObjectId> objectIds = ids.stream()
                .map(ObjectId::new)
                .collect(Collectors.toSet());

        final DBObject query = BasicDBObjectBuilder.start()
                .push(DashboardImpl.FIELD_ID)
                .append("$in", objectIds)
                .get();
        final List<DBObject> results = query(DashboardImpl.class, query);

        final Stream<Dashboard> dashboardStream = results.stream()
                .map(o -> (Dashboard) new DashboardImpl((ObjectId) o.get(DashboardImpl.FIELD_ID), o.toMap()));
        return dashboardStream
                .collect(Collectors.toSet());
    }

    @Override
    public void updateWidgetPositions(Dashboard dashboard, WidgetPositionsRequest positions) throws ValidationException {
        checkNotNull(dashboard, "dashboard must be given");
        checkNotNull(positions, "positions must be given");

        final List<WidgetPosition> widgetPositions = new ArrayList<>(positions.positions().size());

        for (WidgetPositionsRequest.WidgetPosition position : positions.positions()) {
            widgetPositions.add(WidgetPosition.builder()
                    .id(position.id())
                    .width(position.width())
                    .height(position.height())
                    .col(position.col())
                    .row(position.row())
                    .build());
        }

        dashboard.setPositions(widgetPositions);
        save(dashboard);
    }

    @Override
    public void addWidget(Dashboard dashboard, DashboardWidget widget) throws ValidationException {
        embed(dashboard, DashboardImpl.EMBEDDED_WIDGETS, widget);
        dashboard.addWidget(widget);
        clusterEventBus.post(WidgetUpdatedEvent.create(widget));
    }

    @Override
    public void removeWidget(Dashboard dashboard, DashboardWidget widget) {
        removeEmbedded(dashboard, DashboardImpl.EMBEDDED_WIDGETS, widget.getId());
        dashboard.removeWidget(widget);
        clusterEventBus.post(WidgetUpdatedEvent.create(widget));
    }

    @Deprecated
    @Override
    public void updateWidgetDescription(Dashboard dashboard, DashboardWidget widget, String newDescription) throws ValidationException {
        // Updating objects in arrays is a bit flaky in MongoDB. Let'S go the simple and stupid way until weh ave a proper DBA layer.
        widget.setDescription(newDescription);
        removeWidget(dashboard, widget);
        addWidget(dashboard, widget);
    }

    @Deprecated
    @Override
    public void updateWidgetCacheTime(Dashboard dashboard, DashboardWidget widget, int cacheTime) throws ValidationException {
        // Updating objects in arrays is a bit flaky in MongoDB. Let'S go the simple and stupid way until weh ave a proper DBA layer.
        widget.setCacheTime(cacheTime);
        removeWidget(dashboard, widget);
        addWidget(dashboard, widget);
    }

    @Override
    public long count() {
        return totalCount(DashboardImpl.class);
    }

    @Override
    public int destroy(Dashboard dashboard) {
        final String dashboardId = dashboard.getId();
        final Set<String> widgetIds = dashboard.getWidgets().values().stream()
                .map(DashboardWidget::getId)
                .collect(Collectors.toSet());

        final int destroyedDashboards = super.destroy(dashboard);

        for (String widgetId : widgetIds) {
            clusterEventBus.post(WidgetUpdatedEvent.create(widgetId));
        }
        serverEventBus.post(DashboardDeletedEvent.create(dashboardId));

        return destroyedDashboards;
    }
}
