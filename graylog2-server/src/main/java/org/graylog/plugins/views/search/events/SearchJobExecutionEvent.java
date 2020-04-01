/**
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
package org.graylog.plugins.views.search.events;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog2.plugin.database.users.User;
import org.joda.time.DateTime;

@AutoValue
@JsonAutoDetect
public abstract class SearchJobExecutionEvent {
    public abstract User user();
    public abstract SearchJob searchJob();
    public abstract DateTime executionStart();

    public static SearchJobExecutionEvent create(User user, SearchJob searchJob, DateTime executionStart) {
        return new AutoValue_SearchJobExecutionEvent(user, searchJob, executionStart);
    }
}
