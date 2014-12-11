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
    URL: URLUtils.appPrefixed('/dashboards'),
    saveDashboard(dashboard: Dashboard, callback: () => void) {
        var url = this.URL + "/" + dashboard.id + "/update";
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
