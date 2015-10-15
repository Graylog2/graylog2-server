import UserNotification = require("../../util/UserNotification");
import URLUtils = require("../../util/URLUtils");

import jsRoutes = require('routing/jsRoutes');
const fetch = require('logic/rest/FetchProvider').default;

interface Input {
    id: string;
    title: string;
    description: string;
    creatorUser: string;
    createdAt: number;
}

var InputsStore = {
    list(callback: ((inputs: Array<Input>) => void)) {
        var failCallback = (jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Fetching Inputs failed with status: " + errorThrown,
                "Could not retrieve Inputs");
        };

        fetch('GET', URLUtils.qualifyUrl(jsRoutes.controllers.api.InputsApiController.list().url)).then(callback, failCallback);
    },
    globalRecentMessage(input: any, callback: ((message: any) => void)) {
        var failCallback = (jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Loading recent message failed with status: " + errorThrown,
                "Could not retrieve recent message from input \"" + input.title + "\"");
        };

        fetch('GET', URLUtils.qualifyUrl(jsRoutes.controllers.api.InputsApiController.globalRecentMessage(input.id).url)).then(callback, failCallback);
    }
};

export = InputsStore;
