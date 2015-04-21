'use strict';

declare var $: any;
declare var jsRoutes: any;

import UserNotification = require("../../util/UserNotification");
import URLUtils = require("../../util/URLUtils");

var AlarmCallbacksStore = {
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
    },
    save(streamId: string, alarmCallback: any, callback: ((alarmCallback: any) => void)) {
        var failCallback = (jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Saving AlarmCallback failed with status: " + errorThrown,
                "Could not save AlarmCallback!");
        };

        var url = jsRoutes.controllers.api.AlarmCallbacksApiController.create(streamId).url;

        $.ajax({
            type: "POST",
            contentType: "application/json",
            url: url,
            data: JSON.stringify(alarmCallback)
        }).done(callback).fail(failCallback);
    },
    remove(streamId: string, alarmCallbackId: string, callback: (() => void)) {
        var failCallback = (jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Removing AlarmCallback failed with status: " + errorThrown,
                "Could not remove AlarmCallback!");
        };

        var url = jsRoutes.controllers.api.AlarmCallbacksApiController.delete(streamId, alarmCallbackId).url;

        $.ajax({
            type: "DELETE",
            url: url
        }).done(callback).fail(failCallback);
    }
};

export = AlarmCallbacksStore;
