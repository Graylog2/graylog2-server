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
import org.joda.time.DateTime;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class MessageCountAlertCondition extends AlertCondition {

    public enum ThresholdType {
        MORE, LESS
    }

    private int grace;
    private int time;
    private ThresholdType thresholdType;
    private int threshold;

    public MessageCountAlertCondition(Core core, String id, DateTime createdAt, String creatorUserId, Map<String, Object> parameters) {
        super(core, id, Type.MESSAGE_COUNT, createdAt, creatorUserId, parameters);

        this.grace = (Integer) parameters.get("grace");
        this.time = (Integer) parameters.get("time");
        this.thresholdType = ThresholdType.valueOf(((String) parameters.get("threshold_type")).toUpperCase());
        this.threshold = (Integer) parameters.get("threshold");
    }

    @Override
    public String getDescription() {
        return new StringBuilder()
                .append("time: ").append(time)
                .append(", threshold_type: ").append(thresholdType.toString().toLowerCase())
                .append(", threshold: ").append(threshold)
                .append(", grace: ").append(grace)
                .toString();
    }

    @Override
    public boolean triggered() {
        return false;
    }

}
