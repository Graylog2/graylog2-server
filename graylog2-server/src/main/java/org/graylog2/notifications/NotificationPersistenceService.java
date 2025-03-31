package org.graylog2.notifications;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public interface NotificationPersistenceService extends Iterable<Notification> {
    List<Notification> all();

    Optional<Notification> getByTypeAndKey(Notification.Type type, @Nullable String key);

    int destroy(Notification notification);

    int destroyAllByType(Notification.Type type);

    int destroyAllByTypeAndKey(Notification.Type type, @Nullable String key);
}
