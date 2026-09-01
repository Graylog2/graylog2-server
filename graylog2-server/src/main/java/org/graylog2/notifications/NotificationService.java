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
package org.graylog2.notifications;

import org.graylog2.cluster.Node;
import org.graylog2.plugin.database.PersistedService;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public interface NotificationService extends PersistedService {
    Notification build();

    Notification buildNow();

    boolean fixed(Notification.Type type);

    boolean fixed(Notification.Type type, String key);

    /**
     * Remove a notification only if it is owned by the given node. Unlike {@link #fixed(Notification.Type, String)},
     * a node recovering does not clear a notification another node raised for the same key, so a still-failing node's
     * notification survives a peer's recovery.
     * <p>
     * Defaulted so that adding it does not break implementations outside this repository. The default ignores the node
     * and clears cluster-wide, which is the behaviour callers had before this method existed - so an implementation
     * that does not override it is correct but coarser, and callers must not rely on node scoping.
     */
    default boolean fixed(Notification.Type type, String key, String nodeId) {
        return fixed(type, key);
    }

    boolean fixed(Notification.Type type, Node node);

    boolean isFirst(Notification.Type type);

    List<Notification> all();

    Optional<Notification> getByTypeAndKey(Notification.Type type, @Nullable String key);

    boolean publishIfFirst(Notification notification);

    boolean fixed(Notification notification);

    int destroyAllByType(Notification.Type type);

    int destroyAllByTypeAndKey(Notification.Type type, @Nullable String key);
}
