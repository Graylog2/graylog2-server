package lib.notifications;

import org.graylog2.restclient.models.Notification;
import org.graylog2.restclient.models.SystemJob;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class EmailTransportFailedNotification implements NotificationType {
    private final Notification notification;

    public EmailTransportFailedNotification(Notification notification) {
        this.notification = notification;
    }

    @Override
    public Map<SystemJob.Type, String> options() {
        return new HashMap<>();
    }

    @Override
    public String getTitle() {
        return "An error occurred while trying to send an email!";
    }

    @Override
    public String getDescription() {
        return "The Graylog server encountered an error while trying to send an email. " +
                "This is the detailed error message: " + notification.getDetail("exception");
    }

    @Override
    public boolean isCloseable() {
        return true;
    }

    @Override
    public Notification getNotification() {
        return notification;
    }
}
