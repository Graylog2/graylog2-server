import type { PluginExports } from 'graylog-web-plugin/plugin';

import Routes, { SECURITY_ROUTE_DESCRIPTION } from 'routing/Routes';
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

export const navigation = {
  description: SECURITY_ROUTE_DESCRIPTION,
  children: [
    { path: Routes.SECURITY.OVERVIEW, description: 'Overview' },
    { path: Routes.SECURITY.USER_ACTIVITY, description: 'User Activity' },
    { path: Routes.SECURITY.HOST_ACTIVITY, description: 'Host Activity' },
    { path: Routes.SECURITY.NETWORK_ACTIVITY, description: 'Network Activity' },
    { path: Routes.SECURITY.ANOMALIES, description: 'Anomalies' },
  ],
};

const pluginExports: PluginExports = {
  routes,
};

export default pluginExports;
