package views.helpers;

import controllers.routes;
import org.graylog2.restclient.models.Stream;
import org.graylog2.restclient.models.alerts.AlertCondition;
import play.api.mvc.Call;
import play.twirl.api.Html;

public class FieldContentValueAlertConditionDecorator extends AlertConditionDecorator {

    public FieldContentValueAlertConditionDecorator(AlertCondition condition) {
        super(condition);
    }

    public static Html loadTemplate(Stream stream, AlertCondition condition) {
        return views.html.partials.alerts.form_field_content_value.render(stream, new FieldContentValueAlertConditionDecorator(condition));
    }

    @Override
    public Call getFormAction(String streamId) {
        if (isEmptyCondition()) {
            return routes.AlertsController.addTypeFieldContentValue(streamId);
        } else {
            return routes.AlertsController.updateCondition(streamId, getId());
        }
    }

    @Override
    public String getFormId() {
        if (isEmptyCondition()) {
            return "field-content-value";
        } else {
            return "alert-condition-" + getId();
        }
    }

    public String getField() {
        return (String) getParameter("field", "");
    }

    public String getValue() {
        return (String) getParameter("value", "");
    }
}
