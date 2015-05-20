package lib.notifications;

import com.google.common.base.Strings;
import org.graylog2.restclient.models.Notification;
import org.graylog2.restclient.models.SystemJob;

import java.util.Collections;
import java.util.Map;

public class GenericNotification implements NotificationType {
    private final Notification notification;

    public GenericNotification(Notification notification) {
        this.notification = notification;
    }

    @Override
    public Map<SystemJob.Type, String> options() {
        return Collections.emptyMap();
    }

    @Override
    public String getTitle() {
        return Strings.nullToEmpty((String)notification.getDetail("title"));
    }

    @Override
    public String getDescription() {
        return Strings.nullToEmpty((String)notification.getDetail("description"));
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
