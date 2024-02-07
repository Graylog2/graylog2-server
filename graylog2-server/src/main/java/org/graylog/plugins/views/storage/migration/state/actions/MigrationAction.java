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
package org.graylog.plugins.views.storage.migration.state.actions;

import com.github.oxo42.stateless4j.delegates.Action1;

import javax.annotation.Nonnull;
import java.util.Objects;

@FunctionalInterface
@SuppressWarnings("FunctionalInterfaceMethodChanged") // changed for thread safety
public interface MigrationAction extends Action1<MigrationActionContext> {

    @Override
    default void doIt(@Nonnull MigrationActionContext context) {
        synchronized (Objects.requireNonNull(context)) {
            performAction(context);
        }
    }

    void performAction(MigrationActionContext context);

}
