import React from 'react';

import { DocumentTitle, IfPermitted } from 'components/common';
import { IndexerClusterHealth, IndexerFailuresComponent } from 'components/indexers';
import { NotificationsList } from 'components/notifications';
import { SystemJobsComponent } from 'components/systemjobs';
import { SystemMessagesComponent } from 'components/systemmessages';
import { TimesList } from 'components/times';
import { GraylogClusterOverview } from 'components/cluster';
import HideOnCloud from 'util/conditional/HideOnCloud';

class SystemOverviewPage extends React.Component {
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

          <HideOnCloud>
            <IfPermitted permissions="indices:failures">
              <IndexerFailuresComponent />
            </IfPermitted>
          </HideOnCloud>

          <TimesList />

          <IfPermitted permissions="systemmessages:read">
            <SystemMessagesComponent />
          </IfPermitted>
        </span>
      </DocumentTitle>
    );
  }
}

export default SystemOverviewPage;
