import type { PluginExports } from 'graylog-web-plugin/plugin';

import RoutePaths from 'routing/Routes';
import SecurityOverview from 'pages/SecurityOverview';
import TeaserPageLayout from 'components/security/teaser/TeaserPageLayout';

const routes = [
  { path: RoutePaths.SECURITY, component: SecurityOverview, parentComponent: TeaserPageLayout },
];

const pluginExports: PluginExports = {
  routes,
};

export default pluginExports;
