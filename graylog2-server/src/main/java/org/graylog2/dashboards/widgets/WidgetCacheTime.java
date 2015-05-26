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

package org.graylog2.dashboards.widgets;

import com.github.joschi.jadconfig.util.Duration;
import com.google.common.primitives.Ints;
import com.google.inject.assistedinject.Assisted;

import javax.inject.Inject;
import javax.inject.Named;

public class WidgetCacheTime {
    private final int cacheTime;

    public interface Factory {
        WidgetCacheTime create(int cacheTime);
    }

    @Inject
    public WidgetCacheTime(@Named("dashboard_widget_default_cache_time") Duration defaultCacheTime,
                           @Assisted int cacheTime) {
        this.cacheTime = cacheTime < 1 ? Ints.saturatedCast(defaultCacheTime.toSeconds()) : cacheTime;
    }

    public int getCacheTime() {
        return cacheTime;
    }
}
