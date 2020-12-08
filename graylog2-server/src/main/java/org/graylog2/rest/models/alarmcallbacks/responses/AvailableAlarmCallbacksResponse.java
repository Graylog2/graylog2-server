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
package org.graylog2.rest.models.alarmcallbacks.responses;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Maps;
import org.graylog2.rest.models.configuration.responses.RequestedConfigurationField;

import java.util.List;
import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class AvailableAlarmCallbacksResponse {
    public Map<String, AvailableAlarmCallbackSummaryResponse> types;

    @JsonIgnore
    public Map<String, List<RequestedConfigurationField>> getRequestedConfiguration() {
        Map<String, List<RequestedConfigurationField>> result = Maps.newHashMap();

        for (Map.Entry<String, AvailableAlarmCallbackSummaryResponse> entry : types.entrySet()) {
            result.put(entry.getKey(), entry.getValue().extractRequestedConfiguration(entry.getValue().requested_configuration));
        }

        return result;
    }
}
