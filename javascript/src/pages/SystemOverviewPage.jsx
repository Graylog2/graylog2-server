import React from 'react';

import { IfPermitted } from 'components/common';
import NotificationsList from 'components/notifications/NotificationsList';
import { SystemMessagesComponent } from 'components/systemmessages';

const SystemOverviewPage = React.createClass({
  render() {
    return (
      <span>
        <IfPermitted permissions="notifications:read">
          <NotificationsList />
        </IfPermitted>

        <IfPermitted permissions="systemmessages:read">
          <SystemMessagesComponent />
        </IfPermitted>
      </span>
    );
  }
});

export default SystemOverviewPage;
