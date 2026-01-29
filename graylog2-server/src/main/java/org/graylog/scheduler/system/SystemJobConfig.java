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
package org.graylog.scheduler.system;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog.scheduler.JobTriggerData;

/**
 * Marker interface for system job configuration data to abstract from regular job trigger data.
 */
public interface SystemJobConfig extends JobTriggerData {
    interface Builder<SELF> extends JobTriggerData.Builder<SELF> {
        @JsonProperty(TYPE_FIELD)
        SELF type(String type);
    }

    /**
     * Returns a human-readable info string about the job configuration.
     *
     * @return the info object
     */
    SystemJobInfo toInfo();
}
