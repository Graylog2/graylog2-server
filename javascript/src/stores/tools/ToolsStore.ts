/// <reference path="../../../declarations/jquery/jquery.d.ts" />

'use strict';

declare var $: any;
declare var jsRoutes: any;

import UserNotification = require("../../util/UserNotification");

var ToolsStore = {
    testNaturalDate(text: string): JQueryPromise<string[]> {
        var url = jsRoutes.controllers.api.ToolsApiController.naturalDateTest(text).url;

        var promise = $.getJSON(url);

        promise.fail((jqXHR, textStatus, errorThrown) => {
            if (jqXHR.status !== 422) {
                UserNotification.error("Loading keyword preview failed with status: " + errorThrown,
                    "Could not load keyword preview");
            }
        });

        return promise;
    }
};

export = ToolsStore;
