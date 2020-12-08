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
package org.graylog2.inputs.converters;

import org.apache.commons.codec.digest.DigestUtils;
import org.graylog2.plugin.inputs.Converter;

import java.util.Map;

public class HashConverter extends Converter {

    public HashConverter(Map<String, Object> config) {
        super(Type.HASH, config);
    }

    @SuppressWarnings("WEAK_MESSAGE_DIGEST_MD5")
    @Override
    public Object convert(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        // MessageDigest is not threadsafe. #neverForget
        return DigestUtils.md5Hex(value);
    }

    @Override
    public boolean buildsMultipleFields() {
        return false;
    }

}
