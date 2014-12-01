'use strict';

var $ = require('jquery'); // excluded and shimed

var DashboardStore = {
    URL: '/dashboards',
    saveDashboard(dashboard, callback) {
        var url = this.URL + "/" + dashboard.id + "/update";
        $.ajax({
            type: "POST",
            url: url,
            data: dashboard
        }).done(() => {
            callback();
        }).fail((jqXHR, textStatus, errorThrown) => {
            console.error("Saving dashboard " + dashboard.id + " failed with status: " + textStatus);
            console.error("Error", errorThrown);
            alert("Could not save dashboard " + dashboard.title);
        });
    }
};
module.exports = DashboardStore;
