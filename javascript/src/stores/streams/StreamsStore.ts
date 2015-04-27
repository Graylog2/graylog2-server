'use strict';

declare var jsRoutes: any;

import UserNotification = require("../../util/UserNotification");
import URLUtils = require("../../util/URLUtils");

var StreamsStore = {
    load(callback: ((streams: any) => void)) {
        var failCallback = (jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Fetching Streams failed with status: " + errorThrown,
                "Could not retrieve Streams!");
        };

        $.getJSON(jsRoutes.controllers.api.StreamsApiController.list().url, callback).fail(failCallback);
    },
    remove(streamId: string, callback: (() => void)) {
        var failCallback = (jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Removing Stream failed with status: " + errorThrown,
                "Could not remove Stream!");
        };

        var url = jsRoutes.controllers.api.StreamsApiController.delete(streamId).url;
        $.ajax({
            type: "DELETE",
            url: url
        }).done(callback).fail(failCallback);
    }
};

export = StreamsStore;
