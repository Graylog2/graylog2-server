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
package org.graylog.integrations.pagerduty.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * @author Edgar Molina
 *
 */
public class PagerDutyResponse {
    @JsonProperty("status")
    private String status;
    @JsonProperty("message")
    private String message;
    @JsonProperty("dedup_key")
    private String dedupKey;
    @JsonProperty("errors")
    private List<String> errors;

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getDedupKey() {
        return dedupKey;
    }

    public List<String> getErrors() {
        return errors;
    }
}
