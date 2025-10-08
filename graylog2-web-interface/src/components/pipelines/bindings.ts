import type { PluginExports } from 'graylog-web-plugin/plugin';

import Routes from 'routing/Routes';

export const PAGE_NAV_TITLE = 'Pipelines';

const bindings: PluginExports = {
  pageNavigation: [
    {
      description: PAGE_NAV_TITLE,
      children: [
        { description: 'Manage pipelines', path: Routes.SYSTEM.PIPELINES.OVERVIEW, exactPathMatch: true },
        { description: 'Manage rules', path: Routes.SYSTEM.PIPELINES.RULES },
        { description: 'Simulator', path: Routes.SYSTEM.PIPELINES.SIMULATOR },
      ],
    },
  ],
};

export default bindings;
