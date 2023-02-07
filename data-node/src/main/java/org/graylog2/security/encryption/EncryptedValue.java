/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
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
 * // Setting a new password
 * {
 *   "set_value": "set a new password"
 * }
 *
 * // Alternative to set a new password (pass a string instead of an object)
 * "set a new password"
 *
 * // Keep existing value
 * {
 *   "keep_value": true
 * }
 *
 * // Delete existing value
 * {
 *   "delete_value": true
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

    public abstract boolean isKeepValue();

    public abstract boolean isDeleteValue();

    public boolean isSet() {
        return !isNullOrEmpty(value()) && !isNullOrEmpty(salt());
    }

    public static EncryptedValue createUnset() {
        return builder().value("").salt("").isKeepValue(false).isDeleteValue(false).build();
    }

    public static EncryptedValue createWithKeepValue() {
        return builder().value("").salt("").isKeepValue(true).isDeleteValue(false).build();
    }

    public static EncryptedValue createWithDeleteValue() {
        return builder().value("").salt("").isKeepValue(false).isDeleteValue(true).build();
    }

    public static Builder builder() {
        return new AutoValue_EncryptedValue.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder value(String value);

        public abstract Builder salt(String salt);

        public abstract Builder isKeepValue(boolean isKeepValue);

        public abstract Builder isDeleteValue(boolean isDeleteValue);

        public abstract EncryptedValue build();
    }
}
