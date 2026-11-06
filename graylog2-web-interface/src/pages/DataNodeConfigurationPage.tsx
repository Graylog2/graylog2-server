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
import { Navigate } from 'react-router-dom';

import { DocumentTitle, PageHeader } from 'components/common';
import ClusterConfigurationPageNavigation from 'components/cluster-configuration/ClusterConfigurationPageNavigation';
import DataNodeConfigurationEditor from 'components/datanode/configuration/DataNodeConfigurationEditor';
import usePermissions from 'hooks/usePermissions';
import RoutePaths from 'routing/Routes';

const DataNodeConfigurationPage = () => {
  const { isPermitted } = usePermissions();

  if (!isPermitted('clusterconfigentry:read')) {
    return <Navigate to={RoutePaths.NOTFOUND} replace />;
  }

  return (
    <DocumentTitle title="Data Node Configuration">
      <ClusterConfigurationPageNavigation />
      <PageHeader title="Data Node Configuration">
        <span>Configure OpenSearch settings shared by all Data Nodes.</span>
      </PageHeader>
      <DataNodeConfigurationEditor basePath={RoutePaths.SYSTEM.CLUSTER.DATANODE_CONFIGURATION} />
    </DocumentTitle>
  );
};

export default DataNodeConfigurationPage;
