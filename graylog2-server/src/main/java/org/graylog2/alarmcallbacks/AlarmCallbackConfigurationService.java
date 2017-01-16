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

import com.google.inject.ImplementedBy;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.rest.models.alarmcallbacks.requests.CreateAlarmCallbackRequest;

import java.util.List;
import java.util.Map;

@ImplementedBy(AlarmCallbackConfigurationServiceImpl.class)
public interface AlarmCallbackConfigurationService {
    List<AlarmCallbackConfiguration> getForStreamId(String streamId);
    List<AlarmCallbackConfiguration> getForStream(Stream stream);
    AlarmCallbackConfiguration load(String alarmCallbackId);
    AlarmCallbackConfiguration create(String streamId, CreateAlarmCallbackRequest request, String userId);
    long count();
    Map<String, Long> countPerType();
    String save(AlarmCallbackConfiguration model) throws ValidationException;
    int destroy(AlarmCallbackConfiguration model);
}
