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
package org.graylog.events.procedures;

import com.google.inject.assistedinject.Assisted;

public abstract class Action {
    private final String title;
    private final String type;

    protected Action(ActionDto dto) {
        this.title = dto.title();
        this.type = dto.config().type();
    }

    public String title() {
        return title;
    }

    public String type() {
        return type;
    }

    public interface Factory<T extends Action> {
        T create(@Assisted("dto") ActionDto dto);
    }

}
