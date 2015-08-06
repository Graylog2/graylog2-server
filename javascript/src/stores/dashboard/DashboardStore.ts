/// <reference path="../../../declarations/jquery/jquery.d.ts" />
/// <reference path='../../../node_modules/immutable/dist/immutable.d.ts'/>

'use strict';

declare var $: any;
declare var jsRoutes: any;

import Immutable = require('immutable');

import UserNotification = require("../../util/UserNotification");

interface Dashboard {
    id: string;
    description: string;
    title: string;
}

class DashboardStore {
    private _writableDashboards: Immutable.Map<string, Dashboard>;
    private _onWritableDashboardsChanged: {(dashboards: Immutable.Map<string, Dashboard>): void; }[] = [];

    constructor() {
        this._writableDashboards = Immutable.Map<string, Dashboard>();
    }

    get writableDashboards(): Immutable.Map<string, Dashboard> {
        return this._writableDashboards;
    }

    set writableDashboards(newDashboards: Immutable.Map<string, Dashboard>) {
        this._writableDashboards = newDashboards;
        this._emitChange();
    }

    _emitChange() {
        this._onWritableDashboardsChanged.forEach((callback) => callback(this.writableDashboards));
    }

    addOnWritableDashboardsChangedCallback(dashboardChangeCallback: (dashboards: Immutable.Map<string, Dashboard>) => void) {
        this._onWritableDashboardsChanged.push(dashboardChangeCallback);
    }

    updateWritableDashboards() {
        var promise = this.getWritableDashboardList();
        promise.done((dashboards) => this.writableDashboards = Immutable.Map<string, Dashboard>(dashboards));
    }

    listDashboards(): JQueryPromise<string[]> {
        var url = jsRoutes.controllers.api.DashboardsApiController.index().url;
        var promise = $.getJSON(url);
        promise.fail((jqXHR, textStatus, errorThrown) => {
            if (jqXHR.status !== 404) {
                UserNotification.error("Loading dashboard list failed with status: " + errorThrown,
                    "Could not load dashboards");
            }
        });
        return promise;
    }

    getWritableDashboardList(): JQueryPromise<string[]> {
        var url = jsRoutes.controllers.api.DashboardsApiController.listWritable().url;
        var promise = $.getJSON(url);
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
            this.updateWritableDashboards();
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

        promise.done(() => UserNotification.success("Dashboard successfully updated"));
        promise.fail((jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Saving dashboard \"" + dashboard.title + "\" failed with status: " + errorThrown,
                "Could not save dashboard");
        });

        return promise;
    }
}

var dashboardStore = new DashboardStore();
export = dashboardStore;
