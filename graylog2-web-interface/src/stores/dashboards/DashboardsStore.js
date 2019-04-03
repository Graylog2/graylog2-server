import Reflux from 'reflux';
import Immutable from 'immutable';

import ActionsProvider from 'injection/ActionsProvider';
import PaginationHelper from 'util/PaginationHelper';
import ApiRoutes from 'routing/ApiRoutes';
import CombinedProvider from 'injection/CombinedProvider';
import fetch, { fetchPeriodically } from 'logic/rest/FetchProvider';
import PermissionsMixin from 'util/PermissionsMixin';
import URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';

const { CurrentUserStore } = CombinedProvider.get('CurrentUser');
const DashboardsActions = ActionsProvider.getActions('Dashboards');

export default Reflux.createStore({
  listenables: [DashboardsActions],
  dashboards: undefined,
  writableDashboards: undefined,
  permissions: [],

  init() {
    this.listenTo(CurrentUserStore, this.currentUserUpdated, this.currentUserUpdated);
    this.pagination = {
      page: 1,
      perPage: 10,
      total: 0,
      count: 0,
      query: '',
    };
    DashboardsActions.listPage(this.pagination.page, this.pagination.perPage, this.pagination.query);
  },

  currentUserUpdated(state) {
    if (state && state.currentUser) {
      this.permissions = state.currentUser.permissions;
    }
  },

  getInitialState() {
    return {
      dashboards: this.dashboards,
      writableDashboards: this.writableDashboards,
      pagination: this.pagination,
    };
  },

  create(title, description) {
    const url = URLUtils.qualifyUrl(ApiRoutes.DashboardsApiController.create().url);
    const promise = fetch('POST', url, { title: title, description: description })
      .then((response) => {
        UserNotification.success('Dashboard successfully created');
        return response.dashboard_id;
      }, (error) => {
        UserNotification.error(`Creating dashboard "${title}" failed with status: ${error}`, 'Could not create dashboard');
      });

    DashboardsActions.create.promise(promise);

    return promise;
  },

  createCompleted() {
    CurrentUserStore.reload();
    const { page, perPage, query } = this.pagination;
    DashboardsActions.listPage(page, perPage, query);
  },

  delete(dashboard) {
    const url = URLUtils.qualifyUrl(ApiRoutes.DashboardsApiController.delete(dashboard.id).url);
    const promise = fetch('DELETE', url);

    promise.then(() => {
      UserNotification.success('Dashboard successfully deleted');
    }, (error) => {
      UserNotification.error(`Deleting dashboard "${dashboard.title}" failed with status: ${error}`, 'Could not delete dashboard');
    });

    DashboardsActions.delete.promise(promise);

    return promise;
  },

  deleteCompleted() {
    const { page, perPage, query } = this.pagination;
    DashboardsActions.listPage(page, perPage, query);
  },

  get(id) {
    const url = URLUtils.qualifyUrl(ApiRoutes.DashboardsApiController.get(id).url);
    const promise = fetchPeriodically('GET', url);

    promise.catch((error) => {
      if (error.additional && error.additional.status !== 404) {
        UserNotification.error(`Loading your dashboard failed with status: ${error.message}`, 'Could not load your dashboard');
      }
    });

    DashboardsActions.get.promise(promise);

    return promise;
  },

  list() {
    const url = URLUtils.qualifyUrl(ApiRoutes.DashboardsApiController.index().url);
    const promise = fetch('GET', url)
      .then((response) => {
        const dashboardList = Immutable.List(response.dashboards);
        const writableDashboards = this.getWritableDashboardList(dashboardList, this.permissions);

        this.dashboards = dashboardList;
        this.writableDashboards = writableDashboards;
        const state = { dashboards: dashboardList, writableDashboards: writableDashboards };

        this.trigger(state);

        return state;
      }, (error) => {
        if (!error.additional || error.additional.status !== 404) {
          UserNotification.error(`Loading dashboard list failed with status: ${error}`, 'Could not load dashboards');
        }
      });
    DashboardsActions.list.promise(promise);
    return promise;
  },

  listPage(page, perPage, query) {
    const url = PaginationHelper.urlGenerator(ApiRoutes.DashboardsApiController.pageIndex().url, page, perPage, query);
    const promise = fetch('GET', URLUtils.qualifyUrl(url));

    promise.then((response) => {
      this.pagination = {
        count: response.pagination.count,
        total: response.pagination.total,
        page: response.pagination.page,
        perPage: response.pagination.per_page,
        query: response.query,
      };
      this.trigger({
        dashboards: Immutable.List(response.dashboards),
        pagination: this.pagination,
      });
    }, (error) => {
      if (!error.additional || error.additional.status !== 404) {
        UserNotification.error(`Loading dashboard list failed with status: ${error}`, 'Could not load dashboards');
      }
    });
    DashboardsActions.listPage.promise(promise);
    return promise;
  },

  getWritableDashboardList(dashboards, permissions) {
    return dashboards.filter(dashboard => PermissionsMixin.isPermitted(permissions, `dashboards:edit:${dashboard.id}`));
  },

  update(dashboard) {
    const url = URLUtils.qualifyUrl(ApiRoutes.DashboardsApiController.update(dashboard.id).url);
    const promise = fetch('PUT', url, { title: dashboard.title, description: dashboard.description });

    promise.then(() => {
      UserNotification.success('Dashboard successfully updated');
    }, (error) => {
      UserNotification.error(`Saving dashboard "${dashboard.title}" failed with status: ${error}`, 'Could not save dashboard');
    });

    DashboardsActions.update.promise(promise);

    return promise;
  },

  updateCompleted() {
    const { page, perPage, query } = this.pagination;
    DashboardsActions.listPage(page, perPage, query);
  },

  updatePositions(dashboard, positions) {
    const url = URLUtils.qualifyUrl(ApiRoutes.DashboardsApiController.updatePositions(dashboard.id).url);
    const promise = fetch('PUT', url, { positions: positions }).catch((error) => {
      UserNotification.error(`Updating widget positions for dashboard "${dashboard.title}" failed with status: ${error.message}`,
        'Could not update dashboard');

      return error;
    });

    DashboardsActions.updatePositions.promise(promise);

    return promise;
  },
});
