package views.helpers;

import controllers.routes;
import org.graylog2.restclient.models.Stream;
import org.graylog2.restclient.models.alerts.AlertCondition;
import play.api.mvc.Call;
import play.twirl.api.Html;

public class MessageCountAlertConditionDecorator extends AlertConditionDecorator {
    public enum ThresholdType {
        MORE, LESS
    }

    public MessageCountAlertConditionDecorator(AlertCondition condition) {
        super(condition);
    }

    public int getTime() {
        return (int) getParameter("time", 0);
    }

    public int getThreshold() {
        return (int) getParameter("threshold", 0);
    }

    public String getThresholdType() {
        return (String) getParameter("threshold_type", ThresholdType.MORE.toString().toLowerCase());
    }

    public static Html loadTemplate(Stream stream, AlertCondition condition) {
        return views.html.partials.alerts.form_message_count.render(stream, new MessageCountAlertConditionDecorator(condition));
    }

    @Override
    public Call getFormAction(String streamId) {
        if (isEmptyCondition()) {
            return routes.AlertsController.addTypeMessageCount(streamId);
        } else {
            return routes.AlertsController.updateCondition(streamId, getId());
        }
    }

    @Override
    public String getFormId() {
        if (isEmptyCondition()) {
            return "message-count";
        } else {
            return "alert-condition-" + getId();
        }
    }

    public String getThresholdTypesHtml() {
        return super.getThresholdTypesHtml(ThresholdType.values(), ThresholdType.valueOf(getThresholdType().toUpperCase()));
    }
}
