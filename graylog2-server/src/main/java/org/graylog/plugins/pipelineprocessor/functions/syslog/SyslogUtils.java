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
package org.graylog.plugins.pipelineprocessor.functions.syslog;

public final class SyslogUtils {
    /**
     * Converts integer syslog loglevel to human readable string
     *
     * @param level The level to convert
     * @return The human readable level
     * @see <a href="https://tools.ietf.org/html/rfc5424#section-6.2.1">RFC 5424, Section 6.2.1</a>
     */
    public static String levelToString(int level) {
        switch (level) {
            case 0:
                return "Emergency";
            case 1:
                return "Alert";
            case 2:
                return "Critical";
            case 3:
                return "Error";
            case 4:
                return "Warning";
            case 5:
                return "Notice";
            case 6:
                return "Informational";
            case 7:
                return "Debug";
        }

        return "Unknown";
    }

    /**
     * Converts integer syslog facility to human readable string
     *
     * @param facility The facility to convert
     * @return The human readable facility
     * @see <a href="https://tools.ietf.org/html/rfc5424#section-6.2.1">RFC 5424, Section 6.2.1</a>
     */
    public static String facilityToString(int facility) {
        switch (facility) {
            case 0:
                return "kern";
            case 1:
                return "user";
            case 2:
                return "mail";
            case 3:
                return "daemon";
            case 4:
                return "auth";
            case 5:
                return "syslog";
            case 6:
                return "lpr";
            case 7:
                return "news";
            case 8:
                return "uucp";
            case 9:
                return "clock";
            case 10:
                return "authpriv";
            case 11:
                return "ftp";
            case 12:
                return "ntp";
            case 13:
                return "log audit";
            case 14:
                return "log alert";
            case 15:
                return "cron";
            case 16:
                return "local0";
            case 17:
                return "local1";
            case 18:
                return "local2";
            case 19:
                return "local3";
            case 20:
                return "local4";
            case 21:
                return "local5";
            case 22:
                return "local6";
            case 23:
                return "local7";
            default:
                return "Unknown";
        }
    }

    public static int levelFromPriority(int priority) {
        return priority - (facilityFromPriority(priority) << 3);
    }

    public static int facilityFromPriority(int priority) {
        return priority >> 3;
    }
}
