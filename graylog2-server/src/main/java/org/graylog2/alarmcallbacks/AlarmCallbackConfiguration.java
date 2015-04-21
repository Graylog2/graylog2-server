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
package org.graylog2.alarmcallbacks;

import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.database.Persisted;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public interface AlarmCallbackConfiguration extends Persisted {
    void setStream(Stream stream);
    String getStreamId();
    String getType();
    Configuration getConfiguration();
    DateTime getCreatedAt();
    String getCreatorUserId();
}
