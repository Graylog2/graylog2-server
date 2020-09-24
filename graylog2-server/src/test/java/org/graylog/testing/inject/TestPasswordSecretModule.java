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
package org.graylog.testing.inject;

import com.google.inject.name.Names;
import org.graylog2.plugin.inject.Graylog2Module;

public class TestPasswordSecretModule extends Graylog2Module {
    public static final String TEST_PASSWORD_SECRET = "f9a79178-c949-446d-b0ae-c6d5d9a40ba8";

    @Override
    protected void configure() {
        bind(String.class).annotatedWith(Names.named("password_secret")).toInstance(TEST_PASSWORD_SECRET);
    }
}
