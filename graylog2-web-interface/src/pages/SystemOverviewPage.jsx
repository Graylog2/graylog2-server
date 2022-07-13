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
import * as React from 'react';

import { DocumentTitle, IfPermitted } from 'components/common';
import { IndexerClusterHealth } from 'components/indexers';
import IndexerSystemOverviewComponent from 'components/indexers/IndexerSystemOverviewComponent';
import { NotificationsList } from 'components/notifications';
import { SystemJobsComponent } from 'components/systemjobs';
import { SystemMessagesComponent } from 'components/systemmessages';
import { TimesList } from 'components/times';
import { GraylogClusterOverview } from 'components/cluster';
import HideOnCloud from 'util/conditional/HideOnCloud';

const SystemOverviewPage = () => {
  return (
    <DocumentTitle title="System overview">
      <span>
        <IfPermitted permissions="notifications:read">
          <NotificationsList />
        </IfPermitted>

        <HideOnCloud>
          <IfPermitted permissions="systemjobs:read">
            <SystemJobsComponent />
          </IfPermitted>
        </HideOnCloud>

        <GraylogClusterOverview />

        <HideOnCloud>
          <IndexerClusterHealth />

          <IfPermitted permissions="indices:failures">
            <IndexerSystemOverviewComponent />
          </IfPermitted>
        </HideOnCloud>

        <TimesList />

        <IfPermitted permissions="systemmessages:read">
          <SystemMessagesComponent />
        </IfPermitted>
      </span>
    </DocumentTitle>
  );
};

export default SystemOverviewPage;
