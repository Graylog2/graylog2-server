import loadAsync from 'routing/loadAsync';

const DashboardsPage = loadAsync(() => import(/* webpackChunkname: "DashboardsPage" */ './DashboardsPage'));
const ExtendedSearchPage = loadAsync(() => import(/* webpackChunkName: "ExtendedSearchPage" */ './ExtendedSearchPage'));
const NewSearchPage = loadAsync(() => import(/* webpackChunkName: "NewSearchPage" */ './NewSearchPage'));
const ShowViewPage = loadAsync(() => import(/* webpackChunkName: "ShowViewPage" */ './ShowViewPage'));
const ViewManagementPage = loadAsync(() => import(/* webpackChunkName: "ViewManagementPage" */ './ViewManagementPage'));

export {
  DashboardsPage,
  ExtendedSearchPage,
  NewSearchPage,
  ShowViewPage,
  ViewManagementPage,
};
