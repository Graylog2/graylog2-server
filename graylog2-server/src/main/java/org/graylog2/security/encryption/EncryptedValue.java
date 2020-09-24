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
package org.graylog2.security.encryption;

import com.google.auto.value.AutoValue;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * This is a container for encrypted values. It is supposed to be used when storing encrypted values in MongoDB (JSON)
 * and also serialize it in HTTP responses. When storing the value in the database, the JSON serialization looks
 * different than when it's serialized in a HTTP response.
 * <p>
 * Check {@link org.graylog2.security.encryption.EncryptedValueTest} for usage examples.
 * <p>
 * <h3>Expected structure for deserialization <b>without</b> active database attribute</h3>
 * This takes a new value and the {@link EncryptedValueDeserializer} automatically creates an
 * encrypted value for it. (admin wants to set a new password via HTTP request)
 * <pre>{@code
 * {
 *   "set_value": "set a new password"
 * }
 * }</pre>
 *
 * <h3>Expected structure for deserialization <b>with</b> active database attribute</h3>
 * In this case the value is just deserialized as it is. (reading from MongoDB)
 * <pre>{@code
 * {
 *   "encrypted_value": "the encrypted value",
 *   "salt": "the encryption salt"
 * }
 * }</pre>
 *
 * <h3>Serialized structure <b>without</b> active database attribute</h3>
 * In this case the serialized JSON only contains an indicator if a value is set and doesn't contain the
 * encrypted value and the salt. (when value is returned in a HTTP response)
 * <pre>{@code
 * {
 *   "is_set": true
 * }
 * }</pre>
 *
 * <h3>Serialized structure <b>with</b> active database attribute</h3>
 * In this case the serialized JSON contains the encrypted value and the salt. (when storing the value in MongoDB)
 * <pre>{@code
 * {
 *   "encrypted_value": "the encrypted value",
 *   "salt": "the encryption salt"
 * }
 * }</pre>
 *
 * @see EncryptedValueDeserializer
 * @see EncryptedValueSerializer
 */
@AutoValue
public abstract class EncryptedValue {
    public abstract String value();

    public abstract String salt();

    public boolean isSet() {
        return !isNullOrEmpty(value()) && !isNullOrEmpty(salt());
    }

    public static EncryptedValue createUnset() {
        return builder().value("").salt("").build();
    }

    public static Builder builder() {
        return new AutoValue_EncryptedValue.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder value(String value);

        public abstract Builder salt(String salt);

        public abstract EncryptedValue build();
    }
}
