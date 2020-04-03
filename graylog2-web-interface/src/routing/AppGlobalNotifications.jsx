import React from 'react';
import { PluginStore } from 'graylog-web-plugin/plugin';

class AppGlobalNotifications extends React.Component {
  render() {
    const globalNotifications = PluginStore.exports('globalNotifications')
      .map((notification) => {
        if (!notification.component) {
          // eslint-disable-next-line no-console
          console.error('Missing "component" for globalNotification plugin:', notification);
          return null;
        }
        const Component = notification.component;
        if (!notification.key) {
          // eslint-disable-next-line no-console
          console.warn('Missing "key" for globalNotification plugin:', notification);
        }
        return <Component key={notification.key} />;
      })
      .filter((component) => !!component);

    return (
      <div id="global-notifications">
        {globalNotifications}
      </div>
    );
  }
}

export default AppGlobalNotifications;
