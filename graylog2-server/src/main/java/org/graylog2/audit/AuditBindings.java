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
package org.graylog2.audit;

import org.graylog2.audit.formatter.AuditEventFormatter;
import org.graylog2.audit.formatter.FormattedAuditEvent;
import org.graylog2.plugin.PluginModule;

import java.util.Map;

public class AuditBindings extends PluginModule {
    @Override
    protected void configure() {
        // Make sure there is a default binding
        auditEventSenderBinder().setDefault().to(NullAuditEventSender.class);

        addAuditEventTypes(AuditEventTypes.class);

        // Needed to avoid binding errors when there are no implementations of AuditEventFormatter.
        addAuditEventFormatter(AuditEventType.create("__ignore__:__ignore__:__ignore__"), NullAuditEventFormatter.class);
    }

    private static class NullAuditEventFormatter implements AuditEventFormatter {
        @Override
        public FormattedAuditEvent format(AuditActor actor, AuditEventType type, Map<String, Object> context) {
            return null;
        }
    }
}
