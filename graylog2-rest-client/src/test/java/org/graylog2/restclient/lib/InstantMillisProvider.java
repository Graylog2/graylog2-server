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
package org.graylog2.restclient.lib;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstantMillisProvider implements DateTimeUtils.MillisProvider {
    private static final Logger log = LoggerFactory.getLogger(InstantMillisProvider.class);
    private DateTime currentTick;

    public InstantMillisProvider(DateTime instant) {
        setTimeTo(instant);
    }

    public void setTimeTo(DateTime instant) {
        log.debug("Setting clock to {}", instant);
        currentTick = instant;
    }

    @Override
    public long getMillis() {
        return currentTick.getMillis();
    }

    public void tick(Period period) {
        currentTick = currentTick.plus(period);
        log.debug("Ticking clock by {} to {}", period, currentTick);
    }
}
