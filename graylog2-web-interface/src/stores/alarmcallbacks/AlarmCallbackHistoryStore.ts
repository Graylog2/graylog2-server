const UserNotification = require('util/UserNotification');
const URLUtils = require('util/URLUtils');
import jsRoutes = require('routing/jsRoutes');
const fetch = require('logic/rest/FetchProvider').default;

class AlarmCallbackHistoryStore {
    listForAlert(streamId: String, alertId: String) {
        var failCallback = (error) => {
            UserNotification.error("Fetching alarm callback history failed with status: " + error,
                "Could not retrieve alarm callback history.");
        };
        var url = URLUtils.qualifyUrl(jsRoutes.controllers.api.AlarmCallbackHistoryApiController.list(streamId, alertId).url);

        return fetch('GET', url)
          .then(
            response => response.histories,
            failCallback
          );
    }
}
const alarmCallbackHistoryStore = new AlarmCallbackHistoryStore();
export default alarmCallbackHistoryStore;
