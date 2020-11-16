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
package org.graylog.plugins.pipelineprocessor.functions.encoding;

import com.google.common.io.BaseEncoding;

import java.nio.charset.StandardCharsets;

public class Base32HumanEncode extends BaseEncodingSingleArgStringFunction {
    public static final String NAME = "base32human_encode";
    private static final String ENCODING_NAME = "base32 (human-friendly)";

    @Override
    protected String getEncodedValue(String value, boolean omitPadding) {
        BaseEncoding encoding = BaseEncoding.base32();
        encoding = omitPadding ? encoding.omitPadding() : encoding;

        return encoding.encode(value.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected String getEncodingName() {
        return ENCODING_NAME;
    }

    @Override
    protected String getName() {
        return NAME;
    }
}
