/**
 * Copyright 2012-2015 TORCH GmbH, 2015 Graylog, Inc.
 * <p/>
 * This file is part of Graylog.
 * <p/>
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package lib.notifications;

import org.graylog2.restclient.models.Notification;
import org.graylog2.restclient.models.SystemJob;
import views.helpers.DocsHelper;

import java.util.Collections;
import java.util.Map;

public class EsClusterStatusRedNotification implements NotificationType {
    private static final String TITLE = "Elasticsearch cluster unhealthy (RED)";
    private static final String DESCRIPTION = "The Elasticsearch cluster state is RED which means shards are unassigned. "
            + "This usually indicates a crashed and corrupt cluster and needs to be investigated. Graylog will write "
            + "into the local disk journal. Read how to fix this in "
            + DocsHelper.PAGE_ES_CLUSTER_STATUS_RED.toLink("the Elasticsearch setup documentation.");

    private final Notification notification;

    public EsClusterStatusRedNotification(Notification notification) {
        this.notification = notification;
    }

    @Override
    public Notification getNotification() {
        return notification;
    }

    @Override
    public Map<SystemJob.Type, String> options() {
        return Collections.emptyMap();
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public boolean isCloseable() {
        return true;
    }
}
