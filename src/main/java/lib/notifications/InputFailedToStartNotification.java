package lib.notifications;

import com.google.common.collect.Maps;
import models.Notification;
import models.SystemJob;

import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class InputFailedToStartNotification implements NotificationType {
    private final String TITLE;
    private final String DESCRIPTION;

    public InputFailedToStartNotification(Notification notification) {
        // TODO: move this to helper
        DESCRIPTION = "Input " + (String)notification.getDetail("input_id") + " has failed to start on node " +
                notification.getNodeId() + " for this reason: \"" +
                (String)notification.getDetail("reason") + "\". " +
                "This means that you are unable to receive any messages from this input. This is mostly " +
                "an indication for a misconfiguration or an error. " + "" +
                "You can click <a href='routes.InputsController.index()'>here</a> to solve this";
        TITLE = "An input has failed to start.";
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
