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
package org.graylog.plugins.pipelineprocessor.functions.hashing;

import org.apache.commons.codec.digest.DigestUtils;

public class MD5 extends SingleArgStringFunction {

    public static final String NAME = "md5";

    @Override
    protected String getDigest(String value) {
        return DigestUtils.md5Hex(value);
    }

    @Override
    protected String getName() {
        return NAME;
    }
}
