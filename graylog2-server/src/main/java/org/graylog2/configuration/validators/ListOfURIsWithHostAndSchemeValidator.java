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
package org.graylog2.configuration.validators;

import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.Validator;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

public class ListOfURIsWithHostAndSchemeValidator implements Validator<List<URI>> {
    @Override
    public void validate(String name, List<URI> value) throws ValidationException {
        final List<URI> invalidUris = value.stream()
            .filter(uri -> uri.getHost() == null || uri.getScheme() == null)
            .collect(Collectors.toList());

        if (!invalidUris.isEmpty()) {
            throw new ValidationException("Parameter " + name + " must not contain URIs without host or scheme. (found " + invalidUris + ")");
        }
    }
}
