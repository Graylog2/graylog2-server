package lib.notifications;

import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.models.Notification;
import org.graylog2.restclient.models.Stream;
import org.graylog2.restclient.models.StreamService;

import javax.inject.Inject;
import java.io.IOException;

public class NotificationTypeFactory {
    private final StreamService streamService;

    @Inject
    public NotificationTypeFactory(StreamService streamService) {
        this.streamService = streamService;
    }

    public NotificationType get(Notification notification) throws APIException, IOException {
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
            case STREAM_PROCESSING_DISABLED:
                String streamTitle;
                try {
                    final Stream stream = streamService.get(notification.getDetail("stream_id").toString());
                    streamTitle = stream.getTitle();
                } catch (APIException | IOException e) {
                    streamTitle = "(Stream title unavailable)";
                }
                long faultCount = (long) notification.getDetail("fault_count");
                return new StreamProcessingDisabledNotification(notification, streamTitle, faultCount);
        }

        throw new RuntimeException("No notification registered for " + notification.getType());
    }
}