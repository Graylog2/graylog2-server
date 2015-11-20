import React from 'react';

import { IfPermitted } from 'components/common';
import { NotificationsList } from 'components/notifications';
import { TimesList } from 'components/times';
import { SystemMessagesComponent } from 'components/systemmessages';
import { IndexerClusterHealth, IndexerFailuresComponent } from 'components/indexers';
import UsageStatsOptIn from 'components/usagestats/UsageStatsOptIn';

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

        <IfPermitted permissions="indices:failures">
          <IndexerFailuresComponent />
        </IfPermitted>

        <TimesList />

        <IfPermitted permissions="clusterconfigentry:edit:org.graylog.plugins.usagestatistics.UsageStatsOptOutState">
          <UsageStatsOptIn />
        </IfPermitted>

        <IfPermitted permissions="systemmessages:read">
          <SystemMessagesComponent />
        </IfPermitted>
      </span>
    );
  },
});

export default SystemOverviewPage;
