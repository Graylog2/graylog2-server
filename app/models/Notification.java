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
package models;

import com.google.common.collect.Lists;
import lib.APIException;
import lib.Api;
import lib.notifications.DeflectorExistsAsIndexNotification;
import lib.notifications.NotificationType;
import models.api.responses.system.GetNotificationsResponse;
import models.api.responses.system.NotificationSummaryResponse;
import org.joda.time.DateTime;

import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Notification {

    public enum Type {
        DEFLECTOR_EXISTS_AS_INDEX
    }

    public enum Severity {
        NORMAL, URGENT
    }

    private final Type type;
    private final DateTime timestamp;

    public Notification(NotificationSummaryResponse x) {
        this.type = Type.valueOf(x.type.toUpperCase());
        this.timestamp = DateTime.parse(x.timestamp);
    }

    public NotificationType get() {
        switch (type) {
            case DEFLECTOR_EXISTS_AS_INDEX:
                return new DeflectorExistsAsIndexNotification(timestamp);
        }

        throw new RuntimeException("No notification registered for " + type);
    }

    public Type getType() {
        return type;
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    public static List<Notification> all() throws IOException, APIException {
        GetNotificationsResponse r = Api.get("system/notifications", GetNotificationsResponse.class);

        List<Notification> notifications = Lists.newArrayList();
        for (NotificationSummaryResponse notification : r.notifications) {
            notifications.add(new Notification(notification));
        }

        return notifications;
    }

}
