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
package org.graylog2.indexer.cluster.health;

import java.util.Locale;

public class SIUnitParser {
    private static final long C0 = 1L;
    private static final long C1 = C0 * 1024L;
    private static final long C2 = C1 * 1024L;
    private static final long C3 = C2 * 1024L;
    private static final long C4 = C3 * 1024L;
    private static final long C5 = C4 * 1024L;

    private static Long toLong(String value) {
        final String lowerSValue = value.toLowerCase(Locale.ROOT).trim();
        if (lowerSValue.endsWith("k")) {
            return (long) (Double.parseDouble(lowerSValue.substring(0, lowerSValue.length() - 1)) * C1);
        } else if (lowerSValue.endsWith("kb")) {
            return (long) (Double.parseDouble(lowerSValue.substring(0, lowerSValue.length() - 2)) * C1);
        } else if (lowerSValue.endsWith("m")) {
            return (long) (Double.parseDouble(lowerSValue.substring(0, lowerSValue.length() - 1)) * C2);
        } else if (lowerSValue.endsWith("mb")) {
            return (long) (Double.parseDouble(lowerSValue.substring(0, lowerSValue.length() - 2)) * C2);
        } else if (lowerSValue.endsWith("g")) {
            return (long) (Double.parseDouble(lowerSValue.substring(0, lowerSValue.length() - 1)) * C3);
        } else if (lowerSValue.endsWith("gb")) {
            return (long) (Double.parseDouble(lowerSValue.substring(0, lowerSValue.length() - 2)) * C3);
        } else if (lowerSValue.endsWith("t")) {
            return (long) (Double.parseDouble(lowerSValue.substring(0, lowerSValue.length() - 1)) * C4);
        } else if (lowerSValue.endsWith("tb")) {
            return (long) (Double.parseDouble(lowerSValue.substring(0, lowerSValue.length() - 2)) * C4);
        } else if (lowerSValue.endsWith("p")) {
            return (long) (Double.parseDouble(lowerSValue.substring(0, lowerSValue.length() - 1)) * C5);
        } else if (lowerSValue.endsWith("pb")) {
            return (long) (Double.parseDouble(lowerSValue.substring(0, lowerSValue.length() - 2)) * C5);
        } else if (lowerSValue.endsWith("b")) {
            return Long.parseLong(lowerSValue.substring(0, lowerSValue.length() - 1).trim());
        } else if (lowerSValue.equals("-1")) {
            // Allow this special value to be unit-less:
            return -1L;
        } else if (lowerSValue.equals("0")) {
            // Allow this special value to be unit-less:
            return 0L;
        }

        return null;
    }

    public static ByteSize parseBytesSizeValue(String value) {
        final Long longValue = toLong(value);
        if (longValue == null) {
            return null;
        }
        return () -> longValue;
    }
}
