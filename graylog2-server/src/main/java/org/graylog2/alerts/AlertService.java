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
package org.graylog2.alerts;

import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.database.PersistedService;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.rest.models.streams.alerts.requests.CreateConditionRequest;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public interface AlertService extends PersistedService {
    Alert factory(AlertCondition.CheckResult checkResult);

    List<Alert> loadRecentOfStream(String streamId, DateTime since);

    int triggeredSecondsAgo(String streamId, String conditionId);

    long totalCount();
    long totalCountForStream(String streamId);

    AlertCondition fromPersisted(Map<String, Object> conditionFields, Stream stream);
    AlertCondition fromRequest(CreateConditionRequest ccr, Stream stream, String userId);

    AlertCondition updateFromRequest(AlertCondition alertCondition, CreateConditionRequest ccr);

    boolean inGracePeriod(AlertCondition alertCondition);

    AlertCondition.CheckResult triggeredNoGrace(AlertCondition alertCondition);

    AlertCondition.CheckResult triggered(AlertCondition alertCondition);

    Map<String, Object> asMap(final AlertCondition alertCondition);

    List<Alert> listForStreamId(String streamId, int skip, int limit);
    Alert load(String alertId, String streamId) throws NotFoundException;
}
