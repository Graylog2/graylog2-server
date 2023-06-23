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
package org.graylog.aws.inputs.cloudtrail.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

public class CloudTrailRecord implements Serializable {
    @JsonProperty("eventVersion")
    public String eventVersion;
    @JsonProperty("eventTime")
    public String eventTime;

    @JsonProperty("userIdentity")
    public CloudTrailUserIdentity userIdentity;

    //adding responseElements
    @JsonProperty("responseElements")
    public CloudTrailResponseElements responseElements;


    @JsonProperty("eventSource")
    public String eventSource;
    @JsonProperty("eventName")
    public String eventName;
    @JsonProperty("awsRegion")
    public String awsRegion;
    @JsonProperty("sourceIPAddress")
    public String sourceIPAddress;
    @JsonProperty("userAgent")
    public String userAgent;
    @JsonProperty("requestID")
    public String requestID;
    @JsonProperty("eventID")
    public String eventID;
    @JsonProperty("eventType")
    public String eventType;
    @JsonProperty("recipientAccountId")
    public String recipientAccountId;
    @JsonProperty("additionalEventData")
    public Map<String, Object> additionalEventData;

    //adding errorMessage
    @JsonProperty("errorMessage")
    public String errorMessage;

    @JsonProperty("requestParameters")
    public Map<String, Object> requestParameters;

    public Map<String, Object> additionalFieldsAsMap() {
        Map<String, Object> m = Maps.newHashMap();

        m.put("event_source", eventSource);
        m.put("event_name", eventName);
        m.put("aws_region", awsRegion);
        m.put("source_address", sourceIPAddress);
        m.put("user_agent", userAgent);
        m.put("request_id", requestID);
        m.put("event_id", eventID);
        m.put("event_type", eventType);
        m.put("recipient_account_id", recipientAccountId);

        if (additionalEventData != null) {
            m.put("additional_event_data", additionalEventData.toString());
        }

        //adding errorMessage if present
        if (errorMessage != null) {
            m.put("errorMessage", errorMessage);
        }

        if (userIdentity != null) {
            m.putAll(userIdentity.additionalFieldsAsMap());
        }

        //adding responseElements if present
        if (responseElements != null) {
            m.putAll(responseElements.additionalFieldsAsMap());
        }

        return m;
    }

    public String getFullMessage() {
        if (requestParameters != null && !requestParameters.isEmpty()) {
            // Le pretty print.
            return Arrays.toString(requestParameters.entrySet().toArray());
        }

        return null;
    }

    public String getConstructedMessage() {
        return eventSource + ":" + eventName + " in " + awsRegion + " by " + sourceIPAddress + " / " +
                Optional.ofNullable(userIdentity).map(i -> i.userName).orElse("<unknown user_name>");
    }

}
