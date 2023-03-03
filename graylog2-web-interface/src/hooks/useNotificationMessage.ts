import {useStore} from 'stores/connect';
import {useEffect} from 'react';

import NotificationsFactory from 'logic/notifications/NotificationsFactory';
import {NotificationType, NotificationsStore, NotificationsActions} from 'stores/notifications/NotificationsStore';
const useNotificationMessage = (notification: NotificationType) => {
  const { messages } = useStore(NotificationsStore);

  useEffect(() => {
    NotificationsActions.getHtmlMessage(notification.type, notification.key, NotificationsFactory.getValuesForNotification(notification));
  }, []);

  return messages?.[notification.type];
};

export default useNotificationMessage;
