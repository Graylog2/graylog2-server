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

public class NoMasterNotification implements NotificationType {
    @Override
    public Map<SystemJob.Type, String> options() {
        return Maps.newHashMap();
    }

    @Override
    public String getTitle() {
        return "There was no master graylog2-server node detected in the cluster.";
    }

    @Override
    public String getDescription() {
        return "Certain operations of graylog2-server require the presence of a master node, but no such master was started. " +
                "Please ensure that one of your graylog2-server nodes contains the setting <code>is_master = true</code> in its " +
                "configuration and that it is running. Until this is resolved index cycling will not be able to run, which " +
                "means that the index retention mechanism is also not running, leading to increased index sizes. Certain" +
                " maintenance functions as well as a variety of web interface pages (e.g. Dashboards) are unavailable.";
    }

    @Override
    public boolean isCloseable() {
        return false;
    }
}
