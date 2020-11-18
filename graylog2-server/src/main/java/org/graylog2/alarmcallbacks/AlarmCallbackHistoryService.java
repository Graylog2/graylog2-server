/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
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
