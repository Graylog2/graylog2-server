'use strict';

declare var $: any;
declare var jsRoutes: any;

import UserNotification = require("../../util/UserNotification");
import URLUtils = require("../../util/URLUtils");

class AlertsStore {
    list(streamId: String, skip: Number, limit: Number) {
        var failCallback = (jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Fetching alerts failed with status: " + errorThrown,
                "Could not retrieve alerts.");
        };
        var url = jsRoutes.controllers.api.AlertsApiController.list(streamId, skip, limit).url;
        return $.getJSON(url).fail(failCallback);
    }
}
var alertsStore = new AlertsStore();
export = alertsStore;
