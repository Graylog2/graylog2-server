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
package org.graylog2.auditlog;

import com.google.common.base.Joiner;

import javax.inject.Inject;
import javax.inject.Named;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class StdOutAppender implements AuditLogAppender {
    private final boolean enabled;

    @Inject
    public StdOutAppender(@Named("auditlog_stdout_enabled") boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public void write(SuccessStatus successStatus, String subject, String action, String object, Map<String, Object> context) {
        final StringBuilder sb = new StringBuilder();
        sb.append("# AUDIT LOG ENTRY\n");
        sb.append("Status=").append(successStatus).append('\n');
        sb.append("Timestamp=").append(ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)).append('\n');
        sb.append("Subject=").append(subject).append('\n');
        sb.append("Action=").append(action).append('\n');
        sb.append("Object=").append(object).append('\n');
        sb.append("Context=");

        Joiner.on(",")
            .withKeyValueSeparator(":")
            .useForNull("null")
            .appendTo(sb, context);

        System.out.println(sb);
    }
}
