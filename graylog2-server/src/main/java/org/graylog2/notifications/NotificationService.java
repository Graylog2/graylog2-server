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
package org.graylog2.notifications;

import org.graylog2.cluster.Node;
import org.graylog2.database.PersistedService;

import java.util.List;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public interface NotificationService extends PersistedService {
    Notification build();

    Notification buildNow();

    boolean fixed(Notification.Type type);

    boolean fixed(Notification.Type type, Node node);

    boolean isFirst(Notification.Type type);

    List<Notification> all();

    boolean publishIfFirst(Notification notification);

    boolean fixed(Notification notification);

    int destroyAllByType(Notification.Type type);
}
