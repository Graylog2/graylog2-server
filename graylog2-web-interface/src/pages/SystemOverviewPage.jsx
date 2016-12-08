import React from 'react';

import { DocumentTitle, IfPermitted } from 'components/common';
import { IndexerClusterHealth, IndexerFailuresComponent } from 'components/indexers';
import { NotificationsList } from 'components/notifications';
import { SystemJobsComponent } from 'components/systemjobs';
import { SystemMessagesComponent } from 'components/systemmessages';
import { TimesList } from 'components/times';
import UsageStatsOptIn from 'components/usagestats/UsageStatsOptIn';
import { GraylogClusterOverview } from 'components/cluster';

const SystemOverviewPage = React.createClass({
  render() {
    return (
      <DocumentTitle title="System overview">
        <span>
          <IfPermitted permissions="notifications:read">
            <NotificationsList />
          </IfPermitted>

          <IfPermitted permissions="systemjobs:read">
            <SystemJobsComponent />
          </IfPermitted>

          <GraylogClusterOverview />

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
      </DocumentTitle>
    );
  },
});

export default SystemOverviewPage;
