import type { PluginExports } from 'graylog-web-plugin/plugin';

import Routes from 'routing/Routes';

export const PAGE_NAV_TITLE = 'Lookup Tables';

const bindings: PluginExports = {
  pageNavigation: [
    {
      description: PAGE_NAV_TITLE,
      children: [
        { description: 'Lookup Tables', path: Routes.SYSTEM.LOOKUPTABLES.OVERVIEW, exactPathMatch: true },
        { description: 'Caches', path: Routes.SYSTEM.LOOKUPTABLES.CACHES.OVERVIEW },
        { description: 'Data Adapters', path: Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.OVERVIEW },
      ],
    },
  ],
};

export default bindings;
