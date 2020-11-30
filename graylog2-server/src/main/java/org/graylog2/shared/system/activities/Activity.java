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
package org.graylog2.shared.system.activities;

import com.google.common.base.MoreObjects;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

public final class Activity {
    private final Class<?> caller;
    private String message;

    public Activity(Class<?> caller) {
        this.caller = requireNonNull(caller);
    }

    public Activity(String content, Class<?> caller) {
        this.message = requireNonNull(content);
        this.caller = requireNonNull(caller);
    }

    public void setMessage(String message) {
        this.message = requireNonNull(message);
    }

    public String getMessage() {
        return message;
    }

    public Class<?> getCaller() {
        return caller;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Activity activity = (Activity) o;
        return Objects.equals(caller, activity.caller) && Objects.equals(message, activity.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caller, message);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("caller", caller)
            .add("message", message)
            .toString();
    }
}
