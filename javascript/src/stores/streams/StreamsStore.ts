'use strict';

declare var $: any;
declare var jsRoutes: any;

import UserNotification = require("../../util/UserNotification");
import URLUtils = require("../../util/URLUtils");

var StreamsStore = {
	removeOutput(streamId: string, outputId: string, callback: (jqXHR, textStatus, errorThrown) => void) {
        $.ajax({
            url: jsRoutes.controllers.api.StreamOutputsApiController.delete(streamId, outputId).url,
            type: 'DELETE',
            error: (jqXHR, textStatus, errorThrown) => {
                UserNotification.error("Removing output from stream failed with status: " + errorThrown,
                    "Could not remove output from stream");
            },
            success: callback
        });
	}
};
export = StreamsStore;

