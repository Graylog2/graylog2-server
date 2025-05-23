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

import AppConfig from 'util/AppConfig';
import PageNavigation from 'components/common/PageNavigation';
import Routes from 'routing/Routes';
import { Row } from 'components/bootstrap';
import useShowDatanodeMigration from 'components/datanode/hooks/useShowDatanodeMigration';

const ClusterConfigurationPageNavigation = () => {
  const { showDatanodeMigration, isDatanodeConfiguredAndUsed } = useShowDatanodeMigration();
  const enableDataNodeMigration = AppConfig.isFeatureEnabled('data_node_migration');

  const NAV_ITEMS = [
    { description: 'Cluster Configuration', path: Routes.SYSTEM.CLUSTER.NODES, exactPathMatch: true },
    isDatanodeConfiguredAndUsed && {
      description: 'Certificate Management',
      path: Routes.SYSTEM.CLUSTER.CERTIFICATE_MANAGEMENT,
    },
    isDatanodeConfiguredAndUsed && {
      description: 'Data Node Dashboard',
      path: Routes.SYSTEM.CLUSTER.DATANODE_DASHBOARD,
    },
    isDatanodeConfiguredAndUsed && { description: 'Data Node Upgrade', path: Routes.SYSTEM.CLUSTER.DATANODE_UPGRADE },
    showDatanodeMigration &&
      enableDataNodeMigration && { description: 'Data Node Migration', path: Routes.SYSTEM.CLUSTER.DATANODE_MIGRATION },
  ];

  return (
    <Row>
      <PageNavigation items={NAV_ITEMS} />
    </Row>
  );
};

export default ClusterConfigurationPageNavigation;
