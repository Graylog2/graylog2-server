import type { PluginExports } from 'graylog-web-plugin/plugin';

import ThreatIntelPluginConfig from 'threatintel/components/ThreatIntelPluginConfig';
import Routes from 'routing/Routes';
export const PAGE_NAV_TITLE = 'Users';
export const USERS_OVERVIEW_TITLE = 'Users Overview';

const bindings: PluginExports = {
  pageNavigation: [
    {
      description: PAGE_NAV_TITLE,
      children: [
        { description: USERS_OVERVIEW_TITLE, path: Routes.SYSTEM.USERS.OVERVIEW },
        {
          description: 'Token Management',
          path: Routes.SYSTEM.USERS_TOKEN_MANAGEMENT.overview,
          permissions: 'users:tokenlist',
        },
      ],
    },
  ],
};

export default bindings;
