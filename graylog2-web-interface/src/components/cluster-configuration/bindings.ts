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
import type { PluginExports } from 'graylog-web-plugin/plugin';

import Routes from 'routing/Routes';
import useShowDatanodeMigration from 'components/datanode/hooks/useShowDatanodeMigration';
import AppConfig from 'util/AppConfig';

const enableDataNodeMigration = AppConfig.isFeatureEnabled('data_node_migration');
export const PAGE_NAV_TITLE = 'Cluster Configuration';

const bindings: PluginExports = {
  pageNavigation: [
    {
      description: PAGE_NAV_TITLE,
      children: [
        { description: 'Cluster Configuration', path: Routes.SYSTEM.CLUSTER.NODES, exactPathMatch: true },
        {
          description: 'Certificate Management',
          path: Routes.SYSTEM.CLUSTER.CERTIFICATE_MANAGEMENT,
          useCondition: () => {
            const { isDatanodeConfiguredAndUsed } = useShowDatanodeMigration();

            return isDatanodeConfiguredAndUsed;
          },
        },
        {
          description: 'Data Node Dashboard',
          path: Routes.SYSTEM.CLUSTER.DATANODE_DASHBOARD,
          useCondition: () => {
            const { isDatanodeConfiguredAndUsed } = useShowDatanodeMigration();

            return isDatanodeConfiguredAndUsed;
          },
        },
        {
          description: 'Data Node Upgrade',
          path: Routes.SYSTEM.CLUSTER.DATANODE_UPGRADE,
          useCondition: () => {
            const { isDatanodeConfiguredAndUsed } = useShowDatanodeMigration();

            return isDatanodeConfiguredAndUsed;
          },
        },
        ...(enableDataNodeMigration
          ? [
              {
                description: 'Data Node Migration',
                path: Routes.SYSTEM.CLUSTER.DATANODE_MIGRATION,
                useCondition: () => {
                  const { showDatanodeMigration } = useShowDatanodeMigration();

                  return showDatanodeMigration;
                },
              },
            ]
          : []),
      ],
    },
  ],
};

export default bindings;
