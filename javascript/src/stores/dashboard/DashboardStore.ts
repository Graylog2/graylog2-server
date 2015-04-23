/// <reference path="../../../declarations/jquery/jquery.d.ts" />

'use strict';

declare var $: any;

import UserNotification = require("../../util/UserNotification");
import URLUtils = require("../../util/URLUtils");

interface Dashboard {
    id: string;
    description: string;
    title: string;
}

var DashboardStore = {
    getWritableDashboardList(): JQueryPromise<string[]> {
        var url = URLUtils.appPrefixed('/a/dashboards/writable');
        var promise = $.getJSON(url);
        promise.fail((jqXHR, textStatus, errorThrown) => {
            if (jqXHR.status !== 404) {
                UserNotification.error("Loading your dashboard list failed with status: " + errorThrown,
                    "Could not load your dashboard list");
            }
        });
        return promise;
    },
    saveDashboard(dashboard: Dashboard, callback: () => void) {
        var url = URLUtils.appPrefixed('/dashboards') + "/" + dashboard.id + "/update";
        $.ajax({
            type: "POST",
            url: url,
            data: dashboard
        }).done(() => {
            callback();
            UserNotification.success("Dashboard successfully updated");
        }).fail((jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Saving dashboard " + dashboard.id + " failed with status: " + errorThrown,
                "Could not save dashboard " + dashboard.title);
        });
    }
};
export = DashboardStore;
