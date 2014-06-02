package lib.notifications;

import org.graylog2.restclient.models.Notification;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class NotificationTypeFactory {
    public NotificationType get(Notification notification) {
        switch (notification.getType()) {
            case DEFLECTOR_EXISTS_AS_INDEX:
                return new DeflectorExistsAsIndexNotification(notification);
            case MULTI_MASTER:
                return new MultiMasterNotification(notification);
            case NO_MASTER:
                return new NoMasterNotification(notification);
            case ES_OPEN_FILES:
                return new EsOpenFilesNotification(notification);
            case NO_INPUT_RUNNING:
                return new NoInputRunningNotification(notification);
            case INPUT_FAILED_TO_START:
                return new InputFailedToStartNotification(notification);
            case CHECK_SERVER_CLOCKS:
                return new CheckServerClocksNotification(notification);
            case OUTDATED_VERSION:
                return new OutdatedVersionNotification(notification);
            case EMAIL_TRANSPORT_CONFIGURATION_INVALID:
                return new EmailTransportConfigurationInvalidNotification(notification);
            case EMAIL_TRANSPORT_FAILED:
                return new EmailTransportFailedNotification(notification);
        }

        throw new RuntimeException("No notification registered for " + notification.getType());
    }
}
