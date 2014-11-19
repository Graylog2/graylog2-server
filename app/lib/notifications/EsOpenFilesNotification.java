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
package lib.notifications;

import com.google.common.collect.Maps;
import org.graylog2.restclient.models.Notification;
import org.graylog2.restclient.models.SystemJob;
import views.helpers.NotificationHelper;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class EsOpenFilesNotification implements NotificationType {

    private static final String TITLE = "Elasticsearch nodes with too low open file limit";
    private static final String DESCRIPTION = "There are Elasticsearch nodes in the cluster that have a too low " +
                                              "open file limit. (below 64000) This will be causing problems that can be hard to diagnose. " +
                                              "Read how to raise the maximum number of open files in " +
                                              NotificationHelper.linkToKnowledgeBase("configuring-and-tuning-elasticsearch-for-graylog2-v0200", "the documentation.");

    private final Notification notification;

    public EsOpenFilesNotification(Notification notification) {
        this.notification = notification;
    }

    @Override
    public Notification getNotification() {
        return notification;
    }

    @Override
    public Map<SystemJob.Type, String> options() {
        return Maps.newHashMap();
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
