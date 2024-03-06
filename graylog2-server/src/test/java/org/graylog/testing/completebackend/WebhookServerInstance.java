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
package org.graylog.testing.completebackend;

import com.github.rholder.retry.RetryException;

import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

public interface WebhookServerInstance {
    /**
     * @return URI of the request collector, usable in docker inter-container mode, with correct hostname and port
     */
    URI getContainerizedCollectorURI();

    URI getMappedCollectorURI();

    /**
     * @return URI of the webhook tester API. It generally lives on a different port. This URI is mapped to localhost
     * with proper port mapping from outside docker. Use this URI to obtain recorded requests
     */
    URI getMappedApiURI();

    /**
     * @return list of recorded requests from the start of the container.
     */
    List<WebhookRequest> allRequests();

    /**
     * @return list of recorded requests from the start of the container. It will wait till there is at least one request matching the predicate.
     */
    List<WebhookRequest> waitForRequests(Predicate<WebhookRequest> predicate) throws ExecutionException, RetryException;
}
