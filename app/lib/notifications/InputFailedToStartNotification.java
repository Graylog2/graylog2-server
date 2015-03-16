package lib.notifications;

import com.google.common.collect.Maps;
import controllers.routes;
import org.graylog2.restclient.models.Notification;
import org.graylog2.restclient.models.SystemJob;

import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class InputFailedToStartNotification implements NotificationType {
    private final String TITLE;
    private final String DESCRIPTION;
    private final Notification notification;

    public InputFailedToStartNotification(Notification notification) {
        this.notification = notification;
        // TODO: move this to helper
        DESCRIPTION = "Input " + (String)notification.getDetail("input_id") + " has failed to start on node " +
                notification.getNodeId() + " for this reason: \"" +
                (String)notification.getDetail("reason") + "\". " +
                "This means that you are unable to receive any messages from this input. This is mostly " +
                "an indication for a misconfiguration or an error. " + "" +
                "You can click <a href='" + routes.InputsController.index() + "'>here</a> to solve this";
        TITLE = "An input has failed to start";
    }

    @Override
    public Notification getNotification() {
        return notification;
    }

    @Override
    public Map<SystemJob.Type, String> options() {
        return Maps.newHashMap();
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public boolean isCloseable() {
        return true;
    }
}
