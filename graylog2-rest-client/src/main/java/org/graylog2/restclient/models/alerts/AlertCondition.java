/**
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
 */
package org.graylog2.restclient.models.alerts;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.restclient.models.User;
import org.graylog2.restclient.models.UserService;
import org.graylog2.restclient.models.api.responses.alerts.AlertConditionSummaryResponse;
import org.joda.time.DateTime;

import java.text.DecimalFormat;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class AlertCondition {

    public interface Factory {
        public AlertCondition fromSummaryResponse(AlertConditionSummaryResponse acsr);
    }

    public enum Type {
        MESSAGE_COUNT,
        FIELD_VALUE
    }

    private final String id;
    private final Type type;
    private final Map<String, Object> parameters;
    private final boolean inGrace;
    private final DateTime createdAt;
    private final User creatorUser;

    @AssistedInject
    private AlertCondition(UserService userService, @Assisted AlertConditionSummaryResponse acsr) {
        this.id = acsr.id;
        this.type = Type.valueOf(acsr.type.toUpperCase());
        this.parameters = acsr.parameters;
        this.inGrace = acsr.inGrace;
        this.createdAt = DateTime.parse(acsr.createdAt);
        this.creatorUser = userService.load(acsr.creatorUserId);
    }

    public String getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public DateTime getCreatedAt() {
        return createdAt;
    }

    public User getCreatorUser() {
        return creatorUser;
    }

    public boolean isInGrace() {
        return inGrace;
    }

    public String getSummary() {
        switch (type) {
            case MESSAGE_COUNT:
                return "Message count condition";
            case FIELD_VALUE:
                return "Field value condition";
        }

        throw new RuntimeException("Cannot build summary for unknown alert condition type [" + type + "]");
    }

    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        int grace = (int) ((Number) parameters.get("grace")).longValue();
        int backlog = (parameters.get("backlog") != null && parameters.get("backlog") instanceof Number) ?
                (int) ((Number) parameters.get("backlog")).longValue() : 0;


        switch (type) {
            case MESSAGE_COUNT:
                sb.append(buildMessageCountDescription());
                break;
            case FIELD_VALUE:
                sb.append(buildFieldValueDescription());
                break;
            default:
                throw new RuntimeException("Cannot build description for unknown alert condition type [" + type + "]");
        }

        sb.append(buildGraceDescription(grace));
        sb.append(buildBacklogDescription(backlog));

        return sb.toString();
    }

    private String buildBacklogDescription(int backlog) {
        StringBuilder sb = new StringBuilder();

        if (backlog > 0) {
            sb.append(" Including last ");
            if (backlog == 1)
                sb.append(" message");
            else
                sb.append(backlog).append(" messages");
            sb.append(" in alert notification.");
        } else {
            sb.append(" Not including any messages in alert notification.");
        }

        return sb.toString();
    }

    private String buildMessageCountDescription() {
        StringBuilder sb = new StringBuilder();
        int threshold = (int) ((Number) parameters.get("threshold")).longValue();
        int time = (int) ((Number) parameters.get("time")).longValue();

        sb.append("Alert is triggered when there");

        if (threshold == 1) {
            sb.append(" is ");
        } else {
            sb.append(" are ");
        }

        sb.append(parameters.get("threshold_type")).append(" than ").append(threshold);

        if (threshold == 1) {
            sb.append(" message ");
        } else {
            sb.append(" messages ");
        }

        sb.append("in the last ");

        if (time == 1) {
            sb.append("minute. ");
        } else {
            sb.append(time).append(" minutes. ");
        }

        return sb.toString();
    }

    private String buildGraceDescription(int grace) {
        StringBuilder sb = new StringBuilder();
        sb.append("Grace period: ").append(grace);

        if (grace == 1) {
            sb.append(" minute.");
        } else {
            sb.append(" minutes.");
        }

        return sb.toString();
    }

    private String buildFieldValueDescription() {
        StringBuilder sb = new StringBuilder();
        double threshold = ((Number) parameters.get("threshold")).doubleValue();
        String thresholdFormatted = new DecimalFormat("#.###").format(threshold);
        int time = (int) ((Number) parameters.get("time")).longValue();

        sb.append("Alert is triggered when the field ")
                .append(parameters.get("field")).append(" has a ")
                .append(parameters.get("threshold_type"))
                .append(" ");

        if (parameters.get("type").equals("mean") || parameters.get("type").equals("min")
            || parameters.get("type").equals("max")) {
            sb.append(parameters.get("type")).append(" value");
        } else if(parameters.get("type").equals("stddev")) {
            sb.append("standard deviation");
        } else {
            sb.append(parameters.get("type"));
        }

        sb.append(" than ").append(thresholdFormatted)
            .append(" in the last ");

        if (time == 1) {
            sb.append("minute. ");
        } else {
            sb.append(time).append(" minutes. ");
        }

        return sb.toString();
    }

}
