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
package org.graylog.events.processor;

import java.util.Locale;
import java.util.Optional;

public class SystemEntityActionService {

    public Optional<String> findActionDeniedError(SystemEntity entity, SystemEntityAction action, String entityName) {

        if (hasDeniedAction(entity, action)) {
            String error = String.format(Locale.ENGLISH, "Action '%s' is not permitted for '%s' '%s'", action, entityName, entity.id());
            return Optional.of(error);
        }

        return Optional.empty();

    }

    public boolean isViewable(SystemEntity entity) {
        return !hasDeniedAction(entity, SystemEntityAction.VIEW);
    }

    public boolean isListable(SystemEntity entity) {
        return !hasDeniedAction(entity, SystemEntityAction.LIST);
    }
    
    public boolean isExportable(SystemEntity entity) {
        return !hasDeniedAction(entity, SystemEntityAction.EXPORT);
    }

    private boolean hasDeniedAction(SystemEntity entity, SystemEntityAction action) {
        if (entity == null || entity.deniedActions() == null) {
            return false;
        }
        return entity.deniedActions().contains(action);
    }
}
