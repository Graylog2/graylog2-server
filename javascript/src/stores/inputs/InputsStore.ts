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
    load(callback: ((streams: Array<Stream>) => void)) {
        var failCallback = (jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Fetching Inputs failed with status: " + errorThrown,
                "Could not retrieve Inputs!");
        };

        $.getJSON(jsRoutes.controllers.api.InputsApiController.list().url, callback).fail(failCallback);
    }
};

export = InputsStore;
