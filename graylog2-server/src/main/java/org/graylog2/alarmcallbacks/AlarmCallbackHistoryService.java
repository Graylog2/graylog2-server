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
import org.graylog2.alerts.Alert;
import org.graylog2.plugin.alarms.AlertCondition;

import java.util.List;

@ImplementedBy(AlarmCallbackHistoryServiceImpl.class)
public interface AlarmCallbackHistoryService {
    List<AlarmCallbackHistory> getForAlertId(String alertId);
    AlarmCallbackHistory save(AlarmCallbackHistory alarmCallbackHistory);
    AlarmCallbackHistory success(AlarmCallbackConfiguration alarmCallbackConfiguration, Alert alert, AlertCondition alertCondition);
    AlarmCallbackHistory error(AlarmCallbackConfiguration alarmCallbackConfiguration, Alert alert, AlertCondition alertCondition, String error);
}
