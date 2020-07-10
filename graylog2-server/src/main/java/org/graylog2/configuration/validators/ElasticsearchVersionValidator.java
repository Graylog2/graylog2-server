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
import com.google.common.collect.ImmutableList;

import java.util.List;

public class ElasticsearchVersionValidator implements Validator<String> {
    private static final List<String> SUPPORTED_ES_VERSIONS = ImmutableList.of("6", "7");

    @Override
    public void validate(String name, String value) throws ValidationException {
        if (!SUPPORTED_ES_VERSIONS.contains(value)) {
            throw new ValidationException("Invalid Elasticsearch version specified in " + name + ": " + value
                    + ". Supported versions: " + SUPPORTED_ES_VERSIONS);
        }
    }
}
