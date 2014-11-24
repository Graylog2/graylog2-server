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

import org.graylog2.restclient.models.Notification;
import org.graylog2.restclient.models.SystemJob;

import java.util.Collections;
import java.util.Map;

public class GcTooLongNotification implements NotificationType {
    private static final String TITLE = "Nodes with too long GC pauses";
    private static final String DESCRIPTION = "There are Graylog2 nodes on which the garbage collector runs too long. " +
            "Garbage collection runs should be as short as possible. Please check whether those nodes are healthy. " +
            "(Node: <em>%s</em>, GC duration: <em>%s ms</em>, GC threshold: <em>%s ms</em>)";

    private final Notification notification;

    public GcTooLongNotification(Notification notification) {
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
        return String.format(DESCRIPTION,
                notification.getNodeId(),
                notification.getDetail("gc_duration_ms"),
                notification.getDetail("gc_threshold_ms"));
    }

    @Override
    public boolean isCloseable() {
        return true;
    }
}
