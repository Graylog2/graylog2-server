/// <reference path="../../../declarations/jquery/jquery.d.ts" />
/// <reference path='../../../node_modules/immutable/dist/immutable.d.ts'/>
/// <reference path='../../routing/jsRoutes.d.ts' />

'use strict';

import Immutable = require('immutable');
import $ = require('jquery');
import UserNotification = require("util/UserNotification");
import jsRoutes = require('routing/jsRoutes');
import URLUtils = require("../../util/URLUtils");
const fetch = require('logic/rest/FetchProvider').default;

interface Dashboard {
    id: string;
    description: string;
    title: string;
    content_pack: string;
}

class DashboardStore {
    private _writableDashboards: Immutable.Map<string, Dashboard>;
    private _dashboards: Immutable.List<Dashboard>;
    private _onWritableDashboardsChanged: {(dashboards: Immutable.Map<string, Dashboard>): void; }[] = [];
    private _onDashboardsChanged: {(dashboards: Immutable.List<Dashboard>): void; }[] = [];

    constructor() {
        this._dashboards = Immutable.List<Dashboard>();
        this._writableDashboards = Immutable.Map<string, Dashboard>();
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
        var promise = this.getWritableDashboardList();
        promise.done((dashboards) => this.writableDashboards = Immutable.Map<string, Dashboard>(dashboards));
    }

    updateDashboards() {
        var promise = this.listDashboards();
        promise.done((dashboardList) => {
            this.dashboards = dashboardList;
        });
    }

    listDashboards(): JQueryPromise<Immutable.List<Dashboard>> {
        var url = jsRoutes.controllers.api.DashboardsApiController.index().url;
        var promise = fetch('GET', URLUtils.qualifyUrl(url))
          .then((response) => {
            const dashboardList = Immutable.List<Dashboard>(response.dashboards);

            return dashboardList;
          })
          .catch((jqXHR, textStatus, errorThrown) => {
            if (jqXHR.status !== 404) {
                UserNotification.error("Loading dashboard list failed with status: " + errorThrown,
                    "Could not load dashboards");
            }
          });
        return promise;
    }

    getWritableDashboardList(): JQueryPromise<string[]> {
        var url = jsRoutes.controllers.api.DashboardsApiController.listWritable().url;
        var promise = fetch(URLUtils.qualifyUrl(url));
        promise.fail((jqXHR, textStatus, errorThrown) => {
            if (jqXHR.status !== 404) {
                UserNotification.error("Loading your dashboard list failed with status: " + errorThrown,
                    "Could not load your dashboard list");
            }
        });
        return promise;
    }

    createDashboard(title: string, description: string): JQueryPromise<string[]> {
        var url = jsRoutes.controllers.api.DashboardsApiController.create().url;
        var promise = $.ajax({
            type: "POST",
            url: url,
            data: JSON.stringify({title: title, description: description}),
            dataType: 'json',
            contentType: 'application/json'
        });

        promise.done(() => {
            UserNotification.success("Dashboard successfully created");

            if (this._onDashboardsChanged.length > 0) {
                this.updateDashboards();
            } else if (this._onWritableDashboardsChanged.length > 0) {
                this.updateWritableDashboards();
            }
        });
        promise.fail((jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Creating dashboard \"" + title + "\" failed with status: " + errorThrown,
                "Could not create dashboard");
        });

        return promise;
    }

    saveDashboard(dashboard: Dashboard): JQueryPromise<string[]> {
        var url = jsRoutes.controllers.api.DashboardsApiController.update(dashboard.id).url;
        var promise = $.ajax({
            type: "PUT",
            url: url,
            data: JSON.stringify(dashboard),
            dataType: 'json',
            contentType: 'application/json'
        });

        promise.done(() => {
            UserNotification.success("Dashboard successfully updated");

            if (this._onDashboardsChanged.length > 0) {
                this.updateDashboards();
            } else if (this._onWritableDashboardsChanged.length > 0) {
                this.updateWritableDashboards();
            }
        });
        promise.fail((jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Saving dashboard \"" + dashboard.title + "\" failed with status: " + errorThrown,
                "Could not save dashboard");
        });

        return promise;
    }

    remove(dashboard: Dashboard): JQueryPromise<string[]> {
        var url = jsRoutes.controllers.api.DashboardsApiController.delete(dashboard.id).url;
        var promise = $.ajax({
            type: "DELETE",
            url: url
        });

        promise.done(() => {
            UserNotification.success("Dashboard successfully deleted");

            if (this._onDashboardsChanged.length > 0) {
                this.updateDashboards();
            } else if (this._onWritableDashboardsChanged.length > 0) {
                this.updateWritableDashboards();
            }
        });

        promise.fail((jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Deleting dashboard \"" + dashboard.title + "\" failed with status: " + errorThrown,
                "Could not delete dashboard");
        });

        return promise;
    }
}

var dashboardStore = new DashboardStore();
export = dashboardStore;
