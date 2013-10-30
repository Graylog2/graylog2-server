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

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import models.api.responses.dashboards.DashboardSummaryResponse;
import org.joda.time.DateTime;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Dashboard {

    public interface Factory {
        Dashboard fromSummaryResponse(DashboardSummaryResponse dsr);
    }

    private final String id;
    private final String title;
    private final String description;
    private final DateTime createdAt;
    private final User creatorUser;

    @AssistedInject
    private Dashboard(UserService usr, @Assisted DashboardSummaryResponse dsr) {
        this.id = dsr.id;
        this.title = dsr.title;
        this.description = dsr.description;
        this.createdAt = DateTime.parse(dsr.createdAt);
        this.creatorUser = usr.load(dsr.creatorUserId);
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public DateTime getCreatedAt() {
        return createdAt;
    }

    public User getCreatorUser() {
        return creatorUser;
    }

}
