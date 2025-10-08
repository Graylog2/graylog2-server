import type { PluginExports } from 'graylog-web-plugin/plugin';

import Routes from 'routing/Routes';

export const PAGE_NAV_TITLE = 'Sidecar';

const bindings: PluginExports = {
  pageNavigation: [
    {
      description: PAGE_NAV_TITLE,
      children: [
        { description: 'Overview', path: Routes.SYSTEM.SIDECARS.OVERVIEW, exactPathMatch: true },
        { description: 'Administration', path: Routes.SYSTEM.SIDECARS.ADMINISTRATION },
        { description: 'Configuration', path: Routes.SYSTEM.SIDECARS.CONFIGURATION },
        { description: 'Failure Tracking', path: Routes.SYSTEM.SIDECARS.FAILURE_TRACKING },
      ],
    },
  ],
};

export default bindings;
