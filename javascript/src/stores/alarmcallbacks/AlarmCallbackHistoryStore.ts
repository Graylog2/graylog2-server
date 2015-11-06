import UserNotification = require("../../util/UserNotification");
import URLUtils = require("../../util/URLUtils");
import jsRoutes = require('routing/jsRoutes');
const fetch = require('logic/rest/FetchProvider').default;

class AlarmCallbackHistoryStore {
    listForAlert(streamId: String, alertId: String) {
        var failCallback = (jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Fetching alarm callback history failed with status: " + errorThrown,
                "Could not retrieve alarm callback history.");
        };
        var url = URLUtils.qualifyUrl(jsRoutes.controllers.api.AlarmCallbackHistoryApiController.list(streamId, alertId).url);

        return fetch('GET', url).catch(failCallback);
    }
}
const alarmCallbackHistoryStore = new AlarmCallbackHistoryStore();
export default alarmCallbackHistoryStore;
