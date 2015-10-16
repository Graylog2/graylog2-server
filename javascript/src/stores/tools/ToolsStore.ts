import jsRoutes = require('routing/jsRoutes');
import URLUtils = require('util/URLUtils');
import UserNotification = require("util/UserNotification");
const fetch = require('logic/rest/FetchProvider').default;

const ToolsStore = {
    testNaturalDate(text: string): JQueryPromise<string[]> {
        const url = jsRoutes.controllers.api.ToolsApiController.naturalDateTest(text).url;
        const promise = fetch('GET', URLUtils.qualifyUrl(url));

        promise.catch((errorThrown) => {
            if (errorThrown.additional.status !== 422) {
                UserNotification.error("Loading keyword preview failed with status: " + errorThrown.additional.message,
                    "Could not load keyword preview");
            }
        });

        return promise;
    }
};

export = ToolsStore;
