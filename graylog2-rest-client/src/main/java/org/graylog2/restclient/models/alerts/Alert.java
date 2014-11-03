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
package org.graylog2.restclient.models.alerts;

import org.graylog2.restclient.models.api.responses.alerts.AlertSummaryResponse;
import org.joda.time.DateTime;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Alert {

    private final String id;
    private final String streamId;
    private final String conditionId;
    private final DateTime triggeredAt;
    private final String description;
    private final Map<String,Object> conditionParameters;

    public Alert(AlertSummaryResponse asr) {
        this.id = asr.id;
        this.streamId = asr.streamId;
        this.conditionId = asr.conditionId;
        this.triggeredAt = DateTime.parse(asr.triggeredAt);
        this.description = asr.description;
        this.conditionParameters = asr.conditionParameters;
    }

    public Map<String, Object> getConditionParameters() {
        return conditionParameters;
    }

    public String getDescription() {
        return description;
    }

    public DateTime getTriggeredAt() {
        return triggeredAt;
    }

    public String getConditionId() {
        return conditionId;
    }

    public String getStreamId() {
        return streamId;
    }

    public String getId() {
        return id;
    }

}
