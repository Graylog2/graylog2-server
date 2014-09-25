/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.shared.bindings;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import java.util.List;

public final class GuiceInjectorHolder {
    private static Injector injector;

    private GuiceInjectorHolder() {}

    public static Injector createInjector(final List<Module> bindingsModules) {
        if (injector == null) {
            injector = Guice.createInjector(bindingsModules);
        }

        return injector;
    }

    public static Injector getInjector() {
        if (injector == null) {
            throw new IllegalStateException("No injector available. Please call createInjector() first.");
        }

        return injector;
    }
}
