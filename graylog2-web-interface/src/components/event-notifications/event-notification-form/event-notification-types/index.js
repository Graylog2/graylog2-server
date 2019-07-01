import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import EmailNotificationFormContainer from './EmailNotificationFormContainer';

PluginStore.register(new PluginManifest({}, {
  eventNotificationTypes: [
    {
      type: 'email-notification-v1',
      displayName: 'Email Notification',
      formComponent: EmailNotificationFormContainer,
    },
  ],
}));
