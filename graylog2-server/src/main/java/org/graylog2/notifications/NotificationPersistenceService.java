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

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public interface NotificationPersistenceService extends Iterable<Notification> {
    List<Notification> all();

    Optional<Notification> getByTypeAndKey(Notification.Type type, @Nullable String key);

    int destroy(Notification notification);

    int destroyAllByType(Notification.Type type);

    int destroyAllByTypeAndKey(Notification.Type type, @Nullable String key);
}
