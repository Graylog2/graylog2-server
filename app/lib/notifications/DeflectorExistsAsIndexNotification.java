/*
 * Copyright 2012-2015 TORCH GmbH, 2015 Graylog, Inc.
 *
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
package lib.notifications;

import org.graylog2.restclient.models.Notification;
import org.graylog2.restclient.models.SystemJob;
import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class DeflectorExistsAsIndexNotification implements NotificationType {

    private static final String TITLE = "Deflector exists as an index and is not an alias.";
    private static final String DESCRIPTION = "The deflector is meant to be an alias but exists as an index. Multiple " +
            "failures of infrastructure can lead to this. Your messages are still indexed but searches and all " +
            "maintenance tasks will fail or produce incorrect results. It is strongly recommend that you act as soon " +
            "as possible.";
    private final Notification notification;

    public DeflectorExistsAsIndexNotification(Notification notification) {
        this.notification = notification;
    }

    public Notification getNotification() {
        return notification;
    }

    @Override
    public Map<SystemJob.Type, String> options() {
        return new HashMap<SystemJob.Type, String>() {{
            put(SystemJob.Type.FIX_DEFLECTOR_DELETE_INDEX, "Delete the deflector index and re-create the alias. " +
                    "The messages in the deflector index will be lost. Fastest solution.");
            put(SystemJob.Type.FIX_DEFLECTOR_MOVE_INDEX, "Stop message processing, move the deflector index to a regular " +
                    "one and re-create the index. This can take some time and buffer utilization should be monitored.");
        }};
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
