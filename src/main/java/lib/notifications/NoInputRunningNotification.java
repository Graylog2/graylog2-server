package lib.notifications;

import com.google.common.collect.Maps;
import models.SystemJob;

import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class NoInputRunningNotification implements NotificationType {

    private final String TITLE;
    private final String DESCRIPTION;

    public NoInputRunningNotification(String node_id) {
        // TODO: move this to helper
        DESCRIPTION = "There is a node without any running inputs.  " +
                "This means that you are not receiving any messages from this node at this point in time. This is most probably " +
                "an indication of an error or misconfiguration. " + "" +
                "You can click <a href='routes.InputsController.index()'>here</a> to solve this";
        TITLE = "There is a node without any running inputs.";
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
        return false;
    }
}
