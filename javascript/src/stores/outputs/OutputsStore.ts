'use strict';

declare var $: any;
declare var jsRoutes: any;

import UserNotification = require("../../util/UserNotification");
import URLUtils = require("../../util/URLUtils");

interface Output {
    id: string;
    title: string;
    type: string;
}

var OutputsStore = {
    OUTPUTS_URL: jsRoutes.controllers.api.OutputsApiController.index().url,

    load(callback : (outputs: Array<Output>) => void) {
        $.getJSON(this.OUTPUTS_URL, (outputs: Array<Output>) => {
            callback(outputs);
        }).fail(this._failCallback);
    },
    loadForStreamId(streamId: string, callback: (outputs: Array<Output>) => void) {
        $.getJSON(jsRoutes.controllers.api.StreamOutputsApiController.index(streamId).url, (outputs: Array<Output>) => {
            callback(outputs);
        }).fail(this._failCallback);
    },
    loadAvailable(callback: (available: any) => void) {
        $.getJSON(jsRoutes.controllers.api.OutputsApiController.available(), (available: any) => {
            callback(available);
        }).fail(this._failCallback);
    },
    remove(outputId: string, callback: (jqXHR, textStatus, errorThrown) => void) {
        $.ajax({
            url: jsRoutes.controllers.api.OutputsApiController.delete(outputId).url,
            type: 'DELETE',
            error: (jqXHR, textStatus, errorThrown) => {
                UserNotification.error("Terminating output failed with status: " + errorThrown,
                    "Could not terminate output");
            },
            success: callback
        });
    },
    _failCallback(jqXHR: any, textStatus: string, errorThrown: string) {
        UserNotification.error("Loading otuputs failed with status: " + errorThrown,
            "Could not load outputs");
    }
};

export = OutputsStore;
