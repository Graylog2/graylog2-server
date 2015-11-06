import React from 'react';

import { IfPermitted } from 'components/common';
import { NotificationsList } from 'components/notifications';
import { SystemMessagesComponent } from 'components/systemmessages';
import { IndexerClusterHealth } from 'components/indexers';

const SystemOverviewPage = React.createClass({
  render() {
    return (
      <span>
        <IfPermitted permissions="notifications:read">
          <NotificationsList />
        </IfPermitted>

        <IfPermitted permissions="indexercluster:read">
          <IndexerClusterHealth />
        </IfPermitted>

        <IfPermitted permissions="systemmessages:read">
          <SystemMessagesComponent />
        </IfPermitted>
      </span>
    );
  },
});

export default SystemOverviewPage;
