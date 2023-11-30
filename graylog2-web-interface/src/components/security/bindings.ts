import type { PluginExports } from 'graylog-web-plugin/plugin';

import Routes from 'routing/Routes';
import SecurityOverview from 'pages/SecurityOverview';
import TeaserPageLayout from 'components/security/teaser/TeaserPageLayout';
import SecurityUserActivity from 'pages/SecurityUserActivity';
import SecurityHostActivity from 'pages/SecurityHostActivity';
import SecurityNetworkActivity from 'pages/SecurityNetworkActivity';
import SecurityAnomalies from 'pages/SecurityAnomalies';

const routes = [
  { path: Routes.SECURITY.OVERVIEW, component: SecurityOverview, parentComponent: TeaserPageLayout },
  { path: Routes.SECURITY.USER_ACTIVITY, component: SecurityUserActivity, parentComponent: TeaserPageLayout },
  { path: Routes.SECURITY.HOST_ACTIVITY, component: SecurityHostActivity, parentComponent: TeaserPageLayout },
  { path: Routes.SECURITY.NETWORK_ACTIVITY, component: SecurityNetworkActivity, parentComponent: TeaserPageLayout },
  { path: Routes.SECURITY.ANOMALIES, component: SecurityAnomalies, parentComponent: TeaserPageLayout },
];

const pluginExports: PluginExports = {
  routes,
};

export default pluginExports;
