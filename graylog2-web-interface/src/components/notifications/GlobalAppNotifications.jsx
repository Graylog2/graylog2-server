/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import React from 'react';
import { PluginStore } from 'graylog-web-plugin/plugin';

class GlobalAppNotifications extends React.Component {
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

export default GlobalAppNotifications;
