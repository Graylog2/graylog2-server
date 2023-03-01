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
package org.graylog2.inputs;

import java.util.Map;

/**
 * Interface to be used with input configuration request objects. Classes that implement this interface, will be
 * deserialized with the {@link org.graylog2.inputs.jackson.InputConfigurationDeserializer} deserializer which
 * handles transparent {@link org.graylog2.security.encryption.EncryptedValue} deserialization for the input
 * configuration maps.
 *
 * @param <T> the input configuration class
 */
public interface WithInputConfiguration<T> {
    /**
     * Returns the input type string.
     *
     * @return input type
     */
    String type();

    /**
     * Returns the input configuration values map.
     *
     * @return input configuration map
     */
    Map<String, Object> configuration();

    /**
     * Returns and updated value using the given configuration map.
     *
     * @param configuration configuration values
     * @return updated value
     */
    T withConfiguration(Map<String, Object> configuration);
}
