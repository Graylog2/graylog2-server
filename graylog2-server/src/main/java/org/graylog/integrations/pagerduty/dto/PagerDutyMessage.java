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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * @author Edgar Molina
 *
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class PagerDutyMessage {
    @JsonProperty("routing_key")
    private final String routingKey;
    @JsonProperty("event_action")
    private final String eventAction;
    @JsonProperty("dedup_key")
    private final String dedupKey;
    @JsonProperty("client")
    private final String client;
    @JsonProperty("client_url")
    private final String clientUrl;
    @JsonProperty("links")
    private final List<Link> links;
    @JsonProperty("payload")
    private final Map<String, String> payload;

    public PagerDutyMessage(
            String routingKey,
            String eventAction,
            String dedupKey,
            String client,
            String clientUrl,
            List<Link> links,
            Map<String, String> payload) {
        this.routingKey = routingKey;
        this.eventAction = eventAction;
        this.dedupKey = dedupKey;
        this.client = client;
        this.clientUrl = clientUrl;
        this.links = links;
        this.payload = payload;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public String getEventAction() {
        return eventAction;
    }

    public String getDedupKey() {
        return dedupKey;
    }

    public String getClient() {
        return client;
    }

    public String getClientUrl() {
        return clientUrl;
    }

    public List<Link> getLinks() {
        return links;
    }

    public Map<String, String> getPayload() {
        return payload;
    }
}
