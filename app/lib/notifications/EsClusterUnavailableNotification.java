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

public class EsClusterUnavailableNotification implements NotificationType {
    private static final String TITLE = "Elasticsearch cluster unavailable";
    private static final String DESCRIPTION = "Graylog could not successfully connect to the Elasticsearch cluster. "
            + "If you're using multicast, check that it is working in your network and that Elasticsearch is accessible. "
            + "Also check that the cluster name setting is correct. Read how to fix this in " +
            DocsHelper.PAGE_ES_CLUSTER_UNAVAILABLE.toLink("the Elasticsearch setup documentation.");

    private final Notification notification;

    public EsClusterUnavailableNotification(Notification notification) {
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
