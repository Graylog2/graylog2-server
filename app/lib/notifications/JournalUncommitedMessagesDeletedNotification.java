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
package lib.notifications;

import org.graylog2.restclient.models.Notification;
import org.graylog2.restclient.models.SystemJob;

import java.util.Collections;
import java.util.Map;

public class JournalUncommitedMessagesDeletedNotification implements NotificationType {
    private static final String TITLE = "Uncommited messages deleted from journal";
    private static final String DESCRIPTION = "Some messages were deleted from the Graylog journal before they " +
            "could be written to Elasticsearch. Please verify that your Elasticsearch cluster is healthy and fast " +
            "enough. You may also want to review your Graylog journal settings and set a higher limit. " +
            "(Node: <em>%s</em>)";

    private final Notification notification;

    public JournalUncommitedMessagesDeletedNotification(Notification notification) {
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
        return String.format(DESCRIPTION, notification.getNodeId());
    }

    @Override
    public boolean isCloseable() {
        return true;
    }
}
