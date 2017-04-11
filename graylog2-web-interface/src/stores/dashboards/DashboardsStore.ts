/// <reference path='../../../node_modules/immutable/dist/immutable.d.ts'/>
/// <reference path='../../routing/ApiRoutes.d.ts' />
/// <reference path="../../../declarations/bluebird/bluebird.d.ts" />

import Immutable = require('immutable');
const UserNotification = require('util/UserNotification');
import ApiRoutes = require('routing/ApiRoutes');
const URLUtils = require('util/URLUtils');
const Builder = require('logic/rest/FetchProvider').Builder;
const fetchPeriodically = require('logic/rest/FetchProvider').fetchPeriodically;
const fetch = require('logic/rest/FetchProvider').default;
const PermissionsMixin = require('util/PermissionsMixin');

const StoreProvider = require('injection/StoreProvider');
const CurrentUserStore = StoreProvider.getStore('CurrentUser');
const SessionStore = StoreProvider.getStore('Session');

interface Dashboard {
  id: string;
  description: string;
  title: string;
  content_pack: string;
}

class DashboardsStore {
  private _writableDashboards: Immutable.Map<string, Dashboard>;
  private _dashboards: Immutable.List<Dashboard>;
  private _onWritableDashboardsChanged: {(dashboards: Immutable.Map<string, Dashboard>): void; }[] = [];
  private _onDashboardsChanged: {(dashboards: Immutable.List<Dashboard>): void; }[] = [];

  constructor() {
    this._initializeDashboards();
    SessionStore.listen(this.onSessionChange, this);
  }

  _initializeDashboards() {
    this._dashboards = Immutable.List<Dashboard>();
    this._writableDashboards = Immutable.Map<string, Dashboard>();
  }

  onSessionChange() {
    if (!SessionStore.isLoggedIn()) {
      this._initializeDashboards();
    }
  }

  get dashboards(): Immutable.List<Dashboard> {
    return this._dashboards;
  }

  set dashboards(newDashboards: Immutable.List<Dashboard>) {
    this._dashboards = newDashboards;
    this._emitDashboardsChange();
  }

  _emitDashboardsChange() {
    this._onDashboardsChanged.forEach((callback) => callback(this.dashboards));
  }

  get writableDashboards(): Immutable.Map<string, Dashboard> {
    return this._writableDashboards;
  }

  set writableDashboards(newDashboards: Immutable.Map<string, Dashboard>) {
    this._writableDashboards = newDashboards;
    this._emitWritableDashboardsChange();
  }

  _emitWritableDashboardsChange() {
    this._onWritableDashboardsChanged.forEach((callback) => callback(this.writableDashboards));
  }

  addOnWritableDashboardsChangedCallback(dashboardChangeCallback: (dashboards: Immutable.Map<string, Dashboard>) => void) {
    this._onWritableDashboardsChanged.push(dashboardChangeCallback);
  }

  addOnDashboardsChangedCallback(dashboardChangeCallback: (dashboards: Immutable.List<Dashboard>) => void) {
    this._onDashboardsChanged.push(dashboardChangeCallback);
  }

  updateWritableDashboards() {
    const permissions = CurrentUserStore.get().permissions;
    const promise = this.updateDashboards();
    promise.then(() => {
      const dashboards = {};
      this.getWritableDashboardList(permissions).forEach((dashboard) => {
        dashboards[dashboard.id] = dashboard;
      });
      this.writableDashboards = Immutable.Map<string, Dashboard>(dashboards);
    });
  }

  updateDashboards() {
    const promise = this.listDashboards();
    promise.then((dashboardList) => {
      this.dashboards = dashboardList;
    });

    return promise;
  }

  listDashboards(): Promise<Immutable.List<Dashboard>> {
    const url = URLUtils.qualifyUrl(ApiRoutes.DashboardsApiController.index().url);
    const promise = fetch('GET', url)
      .then((response) => {
        const dashboardList = Immutable.List<Dashboard>(response.dashboards);

        return dashboardList;
      }, (error) => {
        if (error.additional.status !== 404) {
          UserNotification.error("Loading dashboard list failed with status: " + error,
            "Could not load dashboards");
        }
      });
    return promise;
  }

  getWritableDashboardList(permissions: Array<string>): Array<Dashboard> {
    return this.dashboards.toArray().filter((dashboard) => PermissionsMixin.isPermitted(permissions, 'dashboards:edit:' + dashboard.id));
  }

  get(id: string): Promise<Dashboard> {
    const url = URLUtils.qualifyUrl(ApiRoutes.DashboardsApiController.get(id).url);
    const promise = fetchPeriodically('GET', url);

    promise.catch((error) => {
      if (error.additional.status !== 404) {
        UserNotification.error("Loading your dashboard failed with status: " + error.message,
          "Could not load your dashboard");
      }
    });

    return promise;
  }

  createDashboard(title: string, description: string): Promise<string[]> {
    const url = URLUtils.qualifyUrl(ApiRoutes.DashboardsApiController.create().url);
    const promise = fetch('POST', url, {title: title, description: description})
      .then((response) => {
        UserNotification.success("Dashboard successfully created");
        this.updateWritableDashboards();
        return response.dashboard_id;
      }, (error) => {
        UserNotification.error("Creating dashboard \"" + title + "\" failed with status: " + error,
          "Could not create dashboard");
      });

    return promise;
  }

  saveDashboard(dashboard: Dashboard): Promise<string[]> {
    const url = URLUtils.qualifyUrl(ApiRoutes.DashboardsApiController.update(dashboard.id).url);
    const promise = fetch('PUT', url, {title: dashboard.title, description: dashboard.description});

    promise.then(() => {
      UserNotification.success("Dashboard successfully updated");
      this.updateWritableDashboards();
    }, (error) => {
      UserNotification.error("Saving dashboard \"" + dashboard.title + "\" failed with status: " + error,
        "Could not save dashboard");
    });

    return promise;
  }

  remove(dashboard: Dashboard): Promise<string[]> {
    const url = URLUtils.qualifyUrl(ApiRoutes.DashboardsApiController.delete(dashboard.id).url);
    const promise = fetch('DELETE', url)

    promise.then(() => {
      UserNotification.success("Dashboard successfully deleted");
      this.updateWritableDashboards();
    }, (error) => {
      UserNotification.error("Deleting dashboard \"" + dashboard.title + "\" failed with status: " + error,
        "Could not delete dashboard");
    });

    return promise;
  }

  updatePositions(dashboard: Dashboard, positions: any) {
    const url = URLUtils.qualifyUrl(ApiRoutes.DashboardsApiController.updatePositions(dashboard.id).url);
    const promise = fetch('PUT', url, {positions: positions}).catch((error) => {
      UserNotification.error("Updating widget positions for dashboard \"" + dashboard.title + "\" failed with status: " + error.message,
        "Could not update dashboard");
    });

    return promise;
  }
}

const dashboardsStore = new DashboardsStore();
export = dashboardsStore;
