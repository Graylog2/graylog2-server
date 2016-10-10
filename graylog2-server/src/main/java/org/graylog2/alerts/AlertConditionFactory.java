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

import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;

import javax.inject.Inject;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

public class AlertConditionFactory {
    private final Map<String, AlertCondition.Factory> alertConditionMap;

    @Inject
    public AlertConditionFactory(Map<String, AlertCondition.Factory> alertConditionMap) {
        this.alertConditionMap = alertConditionMap;
    }

    public AlertCondition createAlertCondition(String type,
                                                Stream stream,
                                                String id,
                                                DateTime createdAt,
                                                String creatorId,
                                                Map<String, Object> parameters,
                                                String title) {

        final AlertCondition.Factory factory = this.alertConditionMap.get(type);
        checkArgument(factory != null, "Unknown alert condition type: " + type);

        return factory.create(stream, id, createdAt, creatorId, parameters, title);
    }
}
