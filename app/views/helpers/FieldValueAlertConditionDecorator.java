package views.helpers;

import controllers.routes;
import org.graylog2.restclient.models.Stream;
import org.graylog2.restclient.models.alerts.AlertCondition;
import play.api.mvc.Call;
import play.twirl.api.Html;

import java.text.DecimalFormat;

public class FieldValueAlertConditionDecorator extends AlertConditionDecorator {
    public enum CheckType {
        MEAN ("mean value"),
        MIN ("minimum value"),
        MAX ("maximum value"),
        SUM ("sum"),
        STDDEV ("standard deviation");

        private String description;

        CheckType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum ThresholdType {
        LOWER, HIGHER
    }

    private final DecimalFormat decimalFormat;

    public FieldValueAlertConditionDecorator(AlertCondition condition) {
        super(condition);
        decimalFormat = new DecimalFormat("#.##");
    }

    public String getField() {
        return (String) getParameter("field", "");
    }

    public int getTime() {
        return (int) getParameter("time", 0);
    }

    public String getThreshold() {
        Number threshold = (Number) getParameter("threshold", 0.0);
        return decimalFormat.format(threshold);
    }

    public String getThresholdType() {
        return (String) getParameter("threshold_type", ThresholdType.LOWER.toString().toLowerCase());
    }

    public String getCheckType() {
        return (String) getParameter("type", CheckType.MEAN.toString().toLowerCase());
    }

    public static Html loadTemplate(Stream stream, AlertCondition condition) {
        return views.html.partials.alerts.form_field_value.render(stream, new FieldValueAlertConditionDecorator(condition));
    }

    @Override
    public Call getFormAction(String streamId) {
        if (isEmptyCondition()) {
            return routes.AlertsController.addTypeFieldValue(streamId);
        } else {
            return routes.AlertsController.updateCondition(streamId, getId());
        }
    }

    @Override
    public String getFormId() {
        if (isEmptyCondition()) {
            return "field-value";
        } else {
            return "alert-condition-" + getId();
        }
    }

    public String getCheckTypesHtml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<select name=\"type\">");

        for (CheckType checkType : CheckType.values()) {
            sb.append("<option value=\"");
            sb.append(checkType.toString().toLowerCase());
            if (checkType == CheckType.valueOf(getCheckType().toUpperCase())) {
                sb.append("\" selected=\"selected\">");
            } else {
                sb.append("\">");
            }
            sb.append(checkType.getDescription());
            sb.append("</option>");
        }

        sb.append("</select>");

        return sb.toString();
    }

    public String getThresholdTypesHtml() {
        return super.getThresholdTypesHtml(ThresholdType.values(), ThresholdType.valueOf(getThresholdType().toUpperCase()));
    }
}
