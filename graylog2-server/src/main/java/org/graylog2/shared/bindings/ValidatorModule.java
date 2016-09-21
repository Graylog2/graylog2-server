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

import com.google.inject.AbstractModule;

import javax.validation.Validation;
import javax.validation.Validator;

public class ValidatorModule extends AbstractModule {
    @Override
    protected void configure() {
        // Validator instances are thread-safe and can be reused.
        // See: http://hibernate.org/validator/documentation/getting-started/
        //
        // The Validator instance creation is quite expensive.
        // Making this a Singleton reduced the CPU load by 50% and reduced the GC load from 5 GCs per second to 2 GCs
        // per second when running a load test of the collector registration endpoint.
        bind(Validator.class).toInstance(Validation.buildDefaultValidatorFactory().getValidator());
    }
}
