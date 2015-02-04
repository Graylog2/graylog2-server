package lib.notifications;

import org.graylog2.restclient.models.Notification;
import org.graylog2.restclient.models.SystemJob;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class EmailTransportConfigurationInvalidNotification implements NotificationType {
    private final Notification notification;

    public EmailTransportConfigurationInvalidNotification(Notification notification) {
        this.notification = notification;
    }

    @Override
    public Map<SystemJob.Type, String> options() {
        return new HashMap<>();
    }

    @Override
    public String getTitle() {
        return "Email Transport Configuration is missing or invalid!";
    }

    @Override
    public String getDescription() {
        return "The configuration for the email transport subsystem has shown to be missing or invalid. " +
                "Please check the related section of your Graylog server configuration file." +
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
