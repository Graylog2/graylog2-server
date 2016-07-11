/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
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
