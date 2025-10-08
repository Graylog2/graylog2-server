import type { PluginExports } from 'graylog-web-plugin/plugin';

import Routes from 'routing/Routes';
import useShowDatanodeMigration from 'components/datanode/hooks/useShowDatanodeMigration';
import AppConfig from 'util/AppConfig';

export const PAGE_NAV_TITLE = 'Indices';

const enableDataNodeMigration = AppConfig.isFeatureEnabled('data_node_migration');

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
