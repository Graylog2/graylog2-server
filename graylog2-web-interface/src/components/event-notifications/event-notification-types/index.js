import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import EmailNotificationFormContainer from './EmailNotificationFormContainer';
import HttpNotificationForm from './HttpNotificationForm';

// eslint-disable-next-line import/prefer-default-export
export const NOTIFICATION_TYPE = 'trigger-notification-v1';

PluginStore.register(new PluginManifest({}, {
  eventNotificationTypes: [
    {
      type: 'email-notification-v1',
      displayName: 'Email Notification',
      formComponent: EmailNotificationFormContainer,
    },
    {
      type: 'http-notification-v1',
      displayName: 'HTTP Notification',
      formComponent: HttpNotificationForm,
    },
  ],
}));
