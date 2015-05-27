package views.helpers;

import org.graylog2.restclient.models.Stream;
import org.graylog2.restclient.models.alerts.AlertCondition;
import play.api.mvc.Call;
import play.twirl.api.Html;

public abstract class AlertConditionDecorator {
    private final AlertCondition alertCondition;

    protected AlertConditionDecorator(AlertCondition condition) {
        this.alertCondition = condition;
    }

    public static Html loadTemplate(Stream stream, AlertCondition condition) {
        switch (condition.getType()) {
            case MESSAGE_COUNT:
                return MessageCountAlertConditionDecorator.loadTemplate(stream, condition);
            case FIELD_VALUE:
                return FieldValueAlertConditionDecorator.loadTemplate(stream, condition);
            case FIELD_CONTENT_VALUE:
                return FieldContentValueAlertConditionDecorator.loadTemplate(stream, condition);
            default:
                return null;
        }
    }

    protected Object getParameter(String key, Object defaultValue) {
        try {
            Object value = alertCondition.getParameters().get(key);
            if (value == null) {
                return defaultValue;
            }

            return value;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public boolean isEmptyCondition() {
        return alertCondition == null;
    }

    public String getId() {
        return alertCondition.getId();
    }

    public int getGrace() {
        return (int) ((Number) getParameter("grace", 0.0)).longValue();
    }

    public int getBacklog() {
        return (int) ((Number) getParameter("backlog", 0.0)).longValue();
    }

    public abstract Call getFormAction(String streamId);

    public abstract String getFormId();

    public String getFormTitle() {
        if (isEmptyCondition()) {
            return "Add new alert condition";
        } else {
            return "Edit alert condition";
        }
    }

    public String getSaveButtonHtml() {
        StringBuilder sb = new StringBuilder();

        sb.append("<button type=\"submit\" class=\"btn btn-success\">");
        if (isEmptyCondition()) {
            sb.append("Add alert condition");
        } else {
            sb.append("Update alert condition");
        }
        sb.append("</button>");

        return sb.toString();
    }

    protected String getThresholdTypesHtml(Enum[] values, Enum selected) {
        StringBuilder sb = new StringBuilder();
        sb.append("<span class=\"threshold-type\">");

        for (Enum thresholdType : values) {
            sb.append("<label class=\"radio-inline\">");
            sb.append("<input type=\"radio\" name=\"threshold_type\" value=\"");
            sb.append(thresholdType.toString().toLowerCase());
            if (thresholdType == selected) {
                sb.append("\" checked=\"checked\">");
            } else {
                sb.append("\">");
            }
            sb.append(thresholdType.toString().toLowerCase());
            sb.append("</label>");
        }

        sb.append("</span>");

        return sb.toString();
    }
}
