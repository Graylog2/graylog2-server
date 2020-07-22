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
package org.graylog.security;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.process.internal.RequestScoped;

// TODO this only works for method injection.
// In theory, by using proxies, it should also work for constructors, etc.
// See: https://stackoverflow.com/a/38060472
public class UserContextBinder extends AbstractBinder {
    @Override
    protected void configure() {
        bindFactory(UserContextFactory.class)
                .to(UserContext.class)
                .in(RequestScoped.class);
    }
}
