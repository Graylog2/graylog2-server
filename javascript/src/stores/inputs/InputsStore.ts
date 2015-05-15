'use strict';

declare var jsRoutes: any;
declare var $: any;

import UserNotification = require("../../util/UserNotification");
import URLUtils = require("../../util/URLUtils");

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
                "Could not retrieve Inputs!");
        };

        $.getJSON(jsRoutes.controllers.api.InputsApiController.list().url, callback).fail(failCallback);
    },
    globalRecentMessage(input: any, callback: ((message: any) => void)) {
        var failCallback = (jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Loading recent message failed with status: " + errorThrown,
                "Could not retrieve recent message from input " + input.title + "!");
        };

        $.getJSON(jsRoutes.controllers.api.InputsApiController.globalRecentMessage(input.id, true).url, callback).fail(failCallback);
    }
};

export = InputsStore;
