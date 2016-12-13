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
package org.graylog2.plugin.events.inputs;

import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.Stoppable;

@AutoValue
@WithBeanGetter
public abstract class IOStateChangedEvent<T extends Stoppable> {
    public abstract IOState.Type oldState();
    public abstract IOState.Type newState();
    public abstract IOState<T> changedState();

    public static <K extends Stoppable> IOStateChangedEvent<K> create(IOState.Type oldState, IOState.Type newState, IOState<K> changedEvent) {
        return new AutoValue_IOStateChangedEvent<>(oldState, newState, changedEvent);
    }
}
