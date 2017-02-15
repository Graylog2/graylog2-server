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
package org.graylog2.shared.bindings;

import com.google.inject.multibindings.Multibinder;
import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.shared.security.ShiroSecurityBinding;

import javax.ws.rs.container.DynamicFeature;

public class RestApiBindings extends Graylog2Module {
    @Override
    protected void configure() {
        bindDynamicFeatures();
        bindContainerResponseFilters();
        // just to create the binders so they are present in the injector
        // we don't actually have global REST API bindings for these
        jerseyExceptionMapperBinder();
        jerseyAdditionalComponentsBinder();
    }

    private void bindDynamicFeatures() {
        Multibinder<Class<? extends DynamicFeature>> setBinder = jerseyDynamicFeatureBinder();
        setBinder.addBinding().toInstance(ShiroSecurityBinding.class);
    }

    private void bindContainerResponseFilters() {
        jerseyContainerResponseFilterBinder();
    }

}
