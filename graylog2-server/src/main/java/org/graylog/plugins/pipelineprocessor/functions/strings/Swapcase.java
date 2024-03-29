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
package org.graylog.plugins.pipelineprocessor.functions.strings;

import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Locale;

public class Swapcase extends StringUtilsFunction {

    public static final String NAME = "swapcase";

    @Override
    protected String getName() {
        return NAME;
    }

    @Override
    protected String description() {
        return "Swaps the case of a String changing upper and title case to lower case, and lower case to upper case.";
    }

    @Override
    protected boolean isLocaleAware() {
        return false;
    }

    @Override
    protected String apply(String value, Locale unused) {
        return StringUtils.swapCase(value);
    }

    @Nonnull
    @Override
    protected String getRuleBuilderName() {
        return "Swap case string";
    }

    @Nonnull
    @Override
    protected String getRuleBuilderTitle() {
        return "Swap the case of '${value}'";
    }
}
