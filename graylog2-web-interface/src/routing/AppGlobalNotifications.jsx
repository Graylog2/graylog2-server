import React from 'react';
import { PluginStore } from 'graylog-web-plugin/plugin';

import styles from './AppGlobalNotifications.css';

const AppGlobalNotifications = React.createClass({
  render() {
    const globalNotifications = PluginStore.exports('globalNotifications')
      .map((notification) => {
        if (!notification.component) {
          console.error('Missing "component" for globalNotification plugin:', notification);
          return null;
        }
        const Component = notification.component;
        if (!notification.key) {
          console.warn('Missing "key" for globalNotification plugin:', notification);
        }
        return <Component key={notification.key} />;
      })
      .filter(component => !!component);

    return (
      <div className={`container-fluid ${styles.globalNotifications}`} id="global-notifications">
        {globalNotifications}
      </div>
    );
  },
});

export default AppGlobalNotifications;
