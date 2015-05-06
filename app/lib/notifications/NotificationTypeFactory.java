/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
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
                int faultCount = (int) notification.getDetail("fault_count");
                return new StreamProcessingDisabledNotification(notification, streamTitle, faultCount);
            case GC_TOO_LONG:
                return new GcTooLongNotification(notification);
            case JOURNAL_UTILIZATION_TOO_HIGH:
                return new JournalUtilizationTooHighNotification(notification);
            case JOURNAL_UNCOMMITTED_MESSAGES_DELETED:
                return new JournalUncommitedMessagesDeletedNotification(notification);
            case OUTPUT_DISABLED:
                return new OutputDisabledNotification(notification);
            case GENERIC:
                return new GenericNotification(notification);
        }

        throw new RuntimeException("No notification registered for " + notification.getType());
    }
}
