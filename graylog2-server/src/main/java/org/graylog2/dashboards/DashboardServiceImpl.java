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

import com.google.common.eventbus.EventBus;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.dashboards.events.DashboardDeletedEvent;
import org.graylog2.dashboards.widgets.DashboardWidget;
import org.graylog2.dashboards.widgets.DashboardWidgetCreator;
import org.graylog2.dashboards.widgets.events.WidgetUpdatedEvent;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PersistedServiceImpl;
import org.graylog2.events.ClusterEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    public void removeWidget(Dashboard dashboard, DashboardWidget widget) {
        removeEmbedded(dashboard, DashboardImpl.EMBEDDED_WIDGETS, widget.getId());
        dashboard.removeWidget(widget);
        clusterEventBus.post(WidgetUpdatedEvent.create(widget));
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
