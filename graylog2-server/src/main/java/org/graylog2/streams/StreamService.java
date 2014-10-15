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
package org.graylog2.streams;

import org.graylog2.database.NotFoundException;
import org.graylog2.database.PersistedService;
import org.graylog2.database.ValidationException;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.rest.resources.streams.requests.CreateRequest;

import java.util.List;
import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public interface StreamService extends PersistedService {
    Stream create(Map<String, Object> fields);
    Stream create(CreateRequest request, String userId);
    Stream load(String id) throws NotFoundException;
    void destroy(Stream stream) throws NotFoundException;
    List<Stream> loadAll();
    List<Stream> loadAllEnabled();
    void pause(Stream stream) throws ValidationException;
    void resume(Stream stream) throws ValidationException;
    List<StreamRule> getStreamRules(Stream stream) throws NotFoundException;
    List<Stream> loadAllWithConfiguredAlertConditions();

    List<AlertCondition> getAlertConditions(Stream stream);
    AlertCondition getAlertCondition(Stream stream, String conditionId) throws NotFoundException;
    void addAlertCondition(Stream stream, AlertCondition condition) throws ValidationException;
    void updateAlertCondition(Stream stream, AlertCondition condition) throws ValidationException;

    void removeAlertCondition(Stream stream, String conditionId);

    void addAlertReceiver(Stream stream, String type, String name);
    void removeAlertReceiver(Stream stream, String type, String name);

    void addOutput(Stream stream, Output output);
    void removeOutput(Stream stream, Output output);
    void removeOutputFromAllStreams(Output output);
}
