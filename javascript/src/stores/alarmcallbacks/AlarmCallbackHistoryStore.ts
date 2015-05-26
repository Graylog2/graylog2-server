'use strict';

declare var $: any;
declare var jsRoutes: any;

import UserNotification = require("../../util/UserNotification");
import URLUtils = require("../../util/URLUtils");

class AlarmCallbackHistoryStore {
    listForAlert(streamId: String, alertId: String) {
        var failCallback = (jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Fetching alarm callback history failed with status: " + errorThrown,
                "Could not retrieve alarm callback history.");
        };
        var url = jsRoutes.controllers.api.AlarmCallbackHistoryApiController.list(streamId, alertId).url;

        return $.getJSON(url).fail(failCallback);
    }
}
var alarmCallbackHistoryStore = new AlarmCallbackHistoryStore();
export = alarmCallbackHistoryStore;
