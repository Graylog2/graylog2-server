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
package org.graylog2.alerts.types;

import org.graylog2.alerts.AbstractAlertCondition;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;

import java.util.Map;

public class DummyAlertCondition extends AbstractAlertCondition {
    final String description = "Dummy alert to test notifications";

    public DummyAlertCondition(Stream stream, String id, DateTime createdAt, String creatorUserId, Map<String, Object> parameters, String title) {
        super(stream, id, Type.DUMMY.toString(), createdAt, creatorUserId, parameters, title);
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public CheckResult runCheck() {
        return new CheckResult(true, this, this.description, Tools.nowUTC(), null);
    }
}
