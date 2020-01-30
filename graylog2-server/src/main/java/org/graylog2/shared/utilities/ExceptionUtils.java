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
package org.graylog2.shared.utilities;

import org.apache.commons.lang3.StringUtils;

import java.net.UnknownHostException;

public class ExceptionUtils {

    public static Throwable getRootCause(Throwable t) {
        return getRootCause(t, false);
    }
    public static Throwable getRootCause(Throwable t, boolean causeNeedsMessage) {
        if (t == null) {
            return null;
        }
        Throwable rootCause = t;
        Throwable cause = rootCause.getCause();
        while (cause != null && (!causeNeedsMessage || StringUtils.isNotBlank(cause.getMessage())) && cause != rootCause) {
            rootCause = cause;
            cause = cause.getCause();
        }
        return rootCause;
    }

    public static String formatMessageCause(Throwable t) {
        if (t == null) {
            return "Unknown cause";
        }
        final StringBuilder causeMessage = new StringBuilder();

        // UnknownHostException has only the hostname as error message, we need to add some
        // information before showing the error message to the user.
        final String message = t.getMessage();
        if (t.getClass() == UnknownHostException.class) {
            causeMessage.append("Unknown host '");
            causeMessage.append(message);
            causeMessage.append("'");
        } else {
            causeMessage.append(message);
        }

        if (message != null && !message.endsWith(".") && !message.endsWith("!")) {
            causeMessage.append(".");
        }

        return causeMessage.toString();
    }

    public static String getRootCauseMessage(Throwable t) {
        return formatMessageCause(getRootCause(t));
    }
    public static String getRootCauseOrMessage(Throwable t) {
        final Throwable rootCause = getRootCause(t, true);
        return formatMessageCause(rootCause != null ? rootCause : t);
    }
}
