import loadAsync from 'routing/loadAsync';

const DashboardsPage = loadAsync(() => import(/* webpackChunkname: "DashboardsPage" */ './DashboardsPage'));
const ExtendedSearchPage = loadAsync(() => import(/* webpackChunkName: "ExtendedSearchPage" */ './ExtendedSearchPage'));
const ViewManagementPage = loadAsync(() => import(/* webpackChunkName: "ViewManagementPage" */ './ViewManagementPage'));
/* eslint-disable import/no-cycle */
const NewSearchPage = loadAsync(() => import(/* webpackChunkName: "NewSearchPage" */ './NewSearchPage'));
const StreamSearchPage = loadAsync(() => import(/* webpackChunkName: "StreamSearchPage" */ './StreamSearchPage'));
const NewDashboardPage = loadAsync(() => import(/* webpackChunkName: "NewDashboardPage" */ './NewDashboardPage'));
const ShowViewPage = loadAsync(() => import(/* webpackChunkName: "ShowViewPage" */ './ShowViewPage'));
/* eslint-enable import/no-cycle */

export {
  DashboardsPage,
  ExtendedSearchPage,
  NewSearchPage,
  ShowViewPage,
  StreamSearchPage,
  NewDashboardPage,
  ViewManagementPage,
};
