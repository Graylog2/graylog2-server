package lib.notifications;

import com.google.common.collect.Maps;
import org.graylog2.restclient.models.Notification;
import org.graylog2.restclient.models.SystemJob;
import views.helpers.NotificationHelper;

import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class StreamProcessingDisabledNotification implements NotificationType {
    private final Notification notification;
    private final String streamTitle;
    private final long faultCount;

    public StreamProcessingDisabledNotification(Notification notification, String streamTitle, long faultCount) {
        this.notification = notification;
        this.streamTitle = streamTitle;
        this.faultCount = faultCount;
    }

    @Override
    public Map<SystemJob.Type, String> options() {
        return Maps.newHashMap();
    }

    @Override
    public String getTitle() {
        return "Processing of a stream has been disabled due to excessive processing time.";
    }

    @Override
    public String getDescription() {
        return "The processing of stream <em>" + streamTitle
                + "</em> has taken too long for " + faultCount + " times. "
                + "To protect the stability of message processing, this stream has been disabled. "
                + "Please correct the stream rules and reenable the stream."
                + " Check " + NotificationHelper.linkToKnowledgeBase("general/streams", "this article")
                + " for more details.";

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
