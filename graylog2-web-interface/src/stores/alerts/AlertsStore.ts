const UserNotification = require('util/UserNotification');
const URLUtils = require('util/URLUtils');
import jsRoutes = require('routing/jsRoutes');
const fetch = require('logic/rest/FetchProvider').default;

class AlertsStore {
    list(streamId: String, skip: Number, limit: Number) {
        var failCallback = (error) => {
            UserNotification.error("Fetching alerts failed with status: " + error.message,
                "Could not retrieve alerts.");
        };
        var url = URLUtils.qualifyUrl(jsRoutes.controllers.api.AlertsApiController.list(streamId, skip, limit).url);
        return fetch('GET', url).catch(failCallback);
    }
}
var alertsStore = new AlertsStore();
export = alertsStore;
