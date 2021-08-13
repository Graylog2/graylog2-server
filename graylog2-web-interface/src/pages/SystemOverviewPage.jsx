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
import { useEffect, useState } from 'react';

import { DocumentTitle, IfPermitted } from 'components/common';
import { IndexerClusterHealth, IndexerFailuresComponent } from 'components/indexers';
import { NotificationsList } from 'components/notifications';
import { SystemJobsComponent } from 'components/systemjobs';
import { SystemMessagesComponent } from 'components/systemmessages';
import { TimesList } from 'components/times';
import { GraylogClusterOverview } from 'components/cluster';
import HideOnCloud from 'util/conditional/HideOnCloud';
import CombinedProvider from 'injection/CombinedProvider';
import usePluginEntities from 'views/logic/usePluginEntities';

const { EnterpriseActions } = CombinedProvider.get('Enterprise');

const SystemOverviewPage = () => {
  const [loadEnterpriseIndexerFailures, setLoadEnterpriseIndexerFailures] = useState(false);
  const pluginSystemOverview = usePluginEntities('systemOverview');
  const EnterpriseIndexerFailures = pluginSystemOverview?.[0]?.component ?? null;

  useEffect(() => {
    if (EnterpriseIndexerFailures) {
      EnterpriseActions.getLicenseInfo().then((response) => {
        setLoadEnterpriseIndexerFailures(response.free_license_info.license_status === 'installed');
      });
    }
  });

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
          <IfPermitted permissions="indexercluster:read">
            <IndexerClusterHealth />
          </IfPermitted>

          <IfPermitted permissions="indices:failures">
            {loadEnterpriseIndexerFailures ? <EnterpriseIndexerFailures /> : <IndexerFailuresComponent />}
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
