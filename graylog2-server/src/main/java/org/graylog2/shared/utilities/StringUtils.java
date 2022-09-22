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
package org.graylog2.shared.utilities;

import java.util.Locale;

public final class StringUtils {

    private StringUtils() {
    }

    public static String f(String format, Object... args) {
        return String.format(Locale.ENGLISH, format, args);
    }

    public static boolean containsOnce(final String mainString, final String subString) {
        if (mainString == null || subString == null) {
            return false;
        }
        final int firstOccurrence = mainString.indexOf(subString);
        if (firstOccurrence == -1) {
            return false;
        }
        final int lastOccurrence = mainString.lastIndexOf(subString);
        return firstOccurrence == lastOccurrence;

    }
}
