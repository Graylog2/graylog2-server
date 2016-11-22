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
package org.graylog2.migrations;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.reflections.Reflections;

public class MigrationsModule extends AbstractModule {
    @Override
    protected void configure() {
        final Multibinder<Migration> binder = Multibinder.newSetBinder(binder(), Migration.class);
        new Reflections(Migration.class.getPackage().getName())
                .getSubTypesOf(Migration.class)
                .forEach(migrationClass -> binder.addBinding().to(migrationClass));
    }
}
