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
package org.graylog2.shared.bindings.providers;

import javax.inject.Provider;
import javax.inject.Singleton;
import javax.validation.Validation;
import javax.validation.Validator;

@Singleton
public class ValidatorProvider implements Provider<Validator> {
    // Validator instances are thread-safe and can be reused.
    // See: http://hibernate.org/validator/documentation/getting-started/
    //
    // The Validator instance creation is quite expensive.
    // Making this a Singleton reduced the CPU load by 50% and reduced the GC load from 5 GCs per second to 2 GCs
    // per second when running a load test of the collector registration endpoint.
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Override
    public Validator get() {
        return validator;
    }
}
