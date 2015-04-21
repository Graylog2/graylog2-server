'use strict';

declare var $: any;
declare var jsRoutes: any;

import UserNotification = require("../../util/UserNotification");
import URLUtils = require("../../util/URLUtils");

var AlarmCallbackStore = {
    available(streamId: string, callback: ((available: any) => void)) {
        var failCallback = (jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Fetching available AlarmCallback types failed with status: " + errorThrown,
                "Could not retrieve available AlarmCallbacks!");
        };

        $.getJSON(jsRoutes.controllers.api.AlarmCallbacksApiController.available(streamId).url, callback).fail(this._failCallback);
    },
    loadForStream(streamId: string, callback: ((alarmCallbacks: any) => void)) {
        var failCallback = (jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Fetching AlarmCallbacks failed with status: " + errorThrown,
                "Could not retrieve AlarmCallbacks!");
        };

        $.getJSON(jsRoutes.controllers.api.AlarmCallbacksApiController.list(streamId).url, callback).fail(this._failCallback);
    }
};

export = AlarmCallbackStore;
