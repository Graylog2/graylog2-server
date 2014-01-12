/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.graylog2.alerts.types;

import org.graylog2.Core;
import org.graylog2.alerts.AlertCondition;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class FieldValueAlertCondition extends AlertCondition {

    private static final Logger LOG = LoggerFactory.getLogger(FieldValueAlertCondition.class);

    public enum CheckType {
        MEAN, MIN, MAX, SUM, STDDEV
    }

    public enum ThresholdType {
        LOWER, HIGHER
    }

    private final int grace;
    private final int time;
    private final ThresholdType thresholdType;
    private final int threshold;
    private final CheckType type;
    private final String field;

    public FieldValueAlertCondition(Core core, Stream stream, String id, DateTime createdAt, String creatorUserId, Map<String, Object> parameters) {
        super(core, stream, id, Type.FIELD_VALUE, createdAt, creatorUserId, parameters);

        this.grace = (Integer) parameters.get("grace");
        this.time = (Integer) parameters.get("time");
        this.thresholdType = ThresholdType.valueOf(((String) parameters.get("threshold_type")).toUpperCase());
        this.threshold = (Integer) parameters.get("threshold");
        this.type = CheckType.valueOf(((String) parameters.get("type")).toUpperCase());
        this.field = (String) parameters.get("field");
    }

    @Override
    public String getDescription() {
        return new StringBuilder()
                .append("time: ").append(time)
                .append(", field: ").append(field)
                .append(", check type: ").append(type.toString().toLowerCase())
                .append(", threshold_type: ").append(thresholdType.toString().toLowerCase())
                .append(", threshold: ").append(threshold)
                .append(", grace: ").append(grace)
                .toString();
    }

    @Override
    protected CheckResult runCheck() {
 System.out.println(getDescription());
        return new CheckResult(false);
    }

}
