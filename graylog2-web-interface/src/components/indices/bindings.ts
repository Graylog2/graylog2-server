import type { PluginExports } from 'graylog-web-plugin/plugin';

import Routes from 'routing/Routes';
import AppConfig from 'util/AppConfig';

export const PAGE_NAV_TITLE = 'Indices';

const PREM_ONLY_NAV_ITEMS = [
  {
    description: 'Index Set Templates',
    path: Routes.SYSTEM.INDICES.TEMPLATES.OVERVIEW,
    exactPathMatch: false,
    permissions: 'indexset_templates:read',
  },
];

const NAV_ITEMS = [
  { description: 'Indices & Index Sets', path: Routes.SYSTEM.INDICES.LIST, exactPathMatch: true },
  {
    description: 'Field Type Profiles',
    path: Routes.SYSTEM.INDICES.FIELD_TYPE_PROFILES.OVERVIEW,
    exactPathMatch: false,
    permissions: 'mappingprofiles:read',
  },
];

const bindings: PluginExports = {
  pageNavigation: [
    {
      description: PAGE_NAV_TITLE,
      children: AppConfig.isCloud() ? NAV_ITEMS : [...NAV_ITEMS, ...PREM_ONLY_NAV_ITEMS],
    },
  ],
};

export default bindings;
