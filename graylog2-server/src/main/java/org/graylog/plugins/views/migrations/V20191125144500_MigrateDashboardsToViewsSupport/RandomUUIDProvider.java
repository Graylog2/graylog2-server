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
package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport;

import com.eaio.uuid.UUIDGen;

import javax.inject.Inject;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class RandomUUIDProvider {
    private final AtomicLong date;
    private final long clockSeqAndNode;

    @Inject
    public RandomUUIDProvider(Date date) {
        this(date, UUIDGen.getClockSeqAndNode());
    }

    public RandomUUIDProvider(Date date, long clockSeqAndNode) {
        this.date = new AtomicLong(date.getTime());
        this.clockSeqAndNode = clockSeqAndNode;
    }

    public String get() {
        return new UUID(date.getAndIncrement(), this.clockSeqAndNode).toString();
    }
}
