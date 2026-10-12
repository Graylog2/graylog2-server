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

package org.graylog.datanode.opensearch;

import com.google.common.eventbus.Subscribe;
import jakarta.annotation.Nonnull;
import org.graylog2.datanode.DataNodeNotficationEvent;
import org.graylog2.events.ClusterEventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Collects the {@link DataNodeNotficationEvent}s posted to a {@link ClusterEventBus} during a test. Doesn't offer
 * any waiting of its own - {@link ClusterEventBus} delivers synchronously, so callers that already have a
 * deterministic way to know the producer is done (e.g. {@link CertificateReloadVerifier#awaitCompletion}) can just
 * read {@link #getNotifications()} straight after that.
 */
public class DatanodeNotificationsReceiver {

    private final List<DataNodeNotficationEvent> receivedNotifications = Collections.synchronizedList(new ArrayList<>());

    public DatanodeNotificationsReceiver(ClusterEventBus clusterEventBus) {
        clusterEventBus.registerClusterEventSubscriber(this);
    }

    @Subscribe
    void onNotification(DataNodeNotficationEvent event) {
        receivedNotifications.add(event);
    }

    @Nonnull
    public List<DataNodeNotficationEvent> getNotifications() {
        return Collections.unmodifiableList(receivedNotifications);
    }
}
