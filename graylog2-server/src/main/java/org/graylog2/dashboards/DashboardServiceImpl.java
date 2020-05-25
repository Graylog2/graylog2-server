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

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PersistedServiceImpl;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DashboardServiceImpl extends PersistedServiceImpl implements DashboardService {
    @Inject
    public DashboardServiceImpl(MongoConnection mongoConnection) {
        super(mongoConnection);
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
    public long count() {
        return totalCount(DashboardImpl.class);
    }
}
