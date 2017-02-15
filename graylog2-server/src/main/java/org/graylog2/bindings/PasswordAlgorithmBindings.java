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
package org.graylog2.bindings;

import com.google.inject.multibindings.MapBinder;
import org.graylog2.bindings.providers.DefaultPasswordAlgorithmProvider;
import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.plugin.security.PasswordAlgorithm;
import org.graylog2.security.hashing.BCryptPasswordAlgorithm;
import org.graylog2.security.hashing.SHA1HashPasswordAlgorithm;
import org.graylog2.users.DefaultPasswordAlgorithm;

public class PasswordAlgorithmBindings extends Graylog2Module {
    @Override
    protected void configure() {
        bindPasswordAlgorithms();
    }

    private void bindPasswordAlgorithms() {
        MapBinder<String, PasswordAlgorithm> passwordAlgorithms = MapBinder.newMapBinder(binder(), String.class, PasswordAlgorithm.class);
        passwordAlgorithms.addBinding("sha-1").to(SHA1HashPasswordAlgorithm.class);
        passwordAlgorithms.addBinding("bcrypt").to(BCryptPasswordAlgorithm.class);

        bind(PasswordAlgorithm.class).annotatedWith(DefaultPasswordAlgorithm.class).toProvider(DefaultPasswordAlgorithmProvider.class);
    }
}
