import loadAsync from 'routing/loadAsync';

const DashboardsPage = loadAsync(() => import(/* webpackChunkname: "DashboardsPage" */ './DashboardsPage'));
const ExtendedSearchPage = loadAsync(() => import(/* webpackChunkName: "ExtendedSearchPage" */ './ExtendedSearchPage'));
const NewSearchPage = loadAsync(() => import(/* webpackChunkName: "NewSearchPage" */ './NewSearchPage'));
const StreamSearchPage = loadAsync(() => import(/* webpackChunkName: "StreamSearchPage" */ './StreamSearchPage'));
const NewDashboardPage = loadAsync(() => import(/* webpackChunkName: "NewDashboardPage" */ './NewDashboardPage'));
const ShowViewPage = loadAsync(() => import(/* webpackChunkName: "ShowViewPage" */ './ShowViewPage'));
const ViewManagementPage = loadAsync(() => import(/* webpackChunkName: "ViewManagementPage" */ './ViewManagementPage'));

export {
  DashboardsPage,
  ExtendedSearchPage,
  NewSearchPage,
  ShowViewPage,
  StreamSearchPage,
  NewDashboardPage,
  ViewManagementPage,
};
