/*
 * Copyright 2013 TORCH GmbH
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
 */
package lib.notifications;

import com.google.common.collect.Maps;
import models.SystemJob;

import java.util.Map;

public class CheckServerClocksNotification implements NotificationType {
    @Override
    public Map<SystemJob.Type, String> options() {
        return Maps.newHashMap();
    }

    @Override
    public String getTitle() {
        return "Check the system clocks of your graylog2-server nodes.";
    }

    @Override
    public String getDescription() {
        return "A graylog2-server node detected a condition where it was deemed to be inactive immediately after being active. " +
                "This usually indicates either a significant jump in system time, e.g. via NTP, or that a second graylog2-server node " +
                "is active on a system that has a different system time. Please make sure that the clocks of graylog2 systems are synchronized.";
    }

    @Override
    public boolean isCloseable() {
        return false;
    }
}
