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
package org.graylog2.plugin;

import com.google.inject.TypeLiteral;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.MapBinder;

public abstract class VersionAwareModule extends PluginModule {
    protected  <T> LinkedBindingBuilder<T> bindForVersion(Version supportedVersion, Class<T> interfaceClass) {
        return mapBinder(interfaceClass).addBinding(supportedVersion);
    }

    private <T> MapBinder<Version, T> mapBinder(Class<T> interfaceClass) {
        return MapBinder.newMapBinder(binder(), Version.class, interfaceClass);
    }

    protected  <T> LinkedBindingBuilder<T> bindForVersion(Version supportedVersion, TypeLiteral<T> interfaceClass) {
        return mapBinder(interfaceClass).addBinding(supportedVersion);
    }

    private <T> MapBinder<Version, T> mapBinder(TypeLiteral<T> interfaceClass) {
        return MapBinder.newMapBinder(binder(), new TypeLiteral<Version>() {}, interfaceClass);
    }
}
