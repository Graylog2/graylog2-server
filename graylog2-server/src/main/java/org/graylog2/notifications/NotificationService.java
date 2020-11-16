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

import java.util.List;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public interface NotificationService extends PersistedService {
    Notification build();

    Notification buildNow();

    boolean fixed(Notification.Type type);

    boolean fixed(Notification.Type type, Node node);

    boolean isFirst(Notification.Type type);

    List<Notification> all();

    boolean publishIfFirst(Notification notification);

    boolean fixed(Notification notification);

    int destroyAllByType(Notification.Type type);
}
