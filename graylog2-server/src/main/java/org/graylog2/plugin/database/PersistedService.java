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
package org.graylog2.plugin.database;

import org.graylog2.plugin.database.validators.ValidationResult;
import org.graylog2.plugin.database.validators.Validator;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public interface PersistedService {
    <T extends Persisted> int destroy(T model);

    <T extends Persisted> int destroyAll(Class<T> modelClass);

    <T extends Persisted> String save(T model) throws ValidationException;

    @Nullable
    <T extends Persisted> String saveWithoutValidation(T model);

    <T extends Persisted> Map<String, List<ValidationResult>> validate(T model, Map<String, Object> fields);

    <T extends Persisted> Map<String, List<ValidationResult>> validate(T model);

    Map<String, List<ValidationResult>> validate(Map<String, Validator> validators, Map<String, Object> fields);
}
