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

import { DocumentTitle, PageHeader } from 'components/common';
import ClusterConfigurationPageNavigation from 'components/cluster-configuration/ClusterConfigurationPageNavigation';
import HideOnCloud from 'util/conditional/HideOnCloud';
import IndexerClusterHealth from 'components/indexers/IndexerClusterHealth';
import ClusterConfigurationNodes from 'components/cluster-configuration/ClusterConfigurationNodes';

const ClusterConfigurationPage = () => (
  <DocumentTitle title="Cluster Configuration">
    <ClusterConfigurationPageNavigation />
    <div>
      <PageHeader title="Cluster Configuration">
        <span>
          This page provides a real-time overview of the nodes in your cluster. You can pause message processing at any
          time. The process buffers will not accept any new messages until you resume it. If the message journal is
          enabled for a node, which it is by default, incoming messages will be persisted to disk, even when processing
          is disabled.
        </span>
      </PageHeader>
      <HideOnCloud>
        <IndexerClusterHealth minimal />
      </HideOnCloud>
      <ClusterConfigurationNodes />
    </div>
  </DocumentTitle>
);

export default ClusterConfigurationPage;
