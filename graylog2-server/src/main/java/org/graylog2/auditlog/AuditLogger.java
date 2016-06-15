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

import com.google.common.collect.ImmutableSet;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class AuditLogger {
    private final Collection<AuditLogAppender> appenders;

    @Inject
    public AuditLogger(Set<AuditLogAppender> appenders) {
        this.appenders = ImmutableSet.copyOf(appenders);
    }

    public void success(String subject, String action, String object) {
        success(subject, action, object, Collections.emptyMap());
    }

    public void success(String subject, String action, String object, Map<String, Object> context) {
        log(SuccessStatus.SUCCESS, subject, action, object, context);
    }

    public void failure(String subject, String action, String object) {
        failure(subject, action, object, Collections.emptyMap());
    }

    public void failure(String subject, String action, String object, Map<String, Object> context) {
        log(SuccessStatus.FAILURE, subject, action, object, context);
    }

    public void log(SuccessStatus successStatus, String subject, String action, String object, Map<String, Object> context) {
        for (AuditLogAppender appender : appenders) {
            if (appender.enabled()) {
                appender.write(successStatus, subject, action, object, context);
            }
        }
    }
}
