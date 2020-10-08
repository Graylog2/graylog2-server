// @flow strict
import history from 'util/History';
import Routes from 'routing/Routes';
import { newDashboardsPath } from 'views/Constants';
import View from 'views/logic/views/View';

export const loadNewView = () => history.push(`${Routes.SEARCH}/new`);

export const loadNewSearch = loadNewView;

export const loadNewViewForStream = (streamId: string) => history.push(`${Routes.stream_search(streamId)}/new`);

export const loadView = (viewId: string) => history.push(`${Routes.SEARCH}/${viewId}`);

export const loadDashboard = (dashboardId: string) => history.push(Routes.pluginRoute('DASHBOARDS_VIEWID')(dashboardId));

export const loadAsDashboard = (view: View) => history.push({
  pathname: newDashboardsPath,
  state: {
    view,
  },
});
