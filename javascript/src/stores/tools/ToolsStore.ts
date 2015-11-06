/// <reference path="../../../declarations/bluebird/bluebird.d.ts" />

import jsRoutes = require('routing/jsRoutes');
import URLUtils = require('util/URLUtils');
import UserNotification = require("util/UserNotification");
const fetch = require('logic/rest/FetchProvider').default;

const ToolsStore = {
    testNaturalDate(text: string): Promise<string[]> {
        const url = jsRoutes.controllers.api.ToolsApiController.naturalDateTest(text).url;
        const promise = fetch('GET', URLUtils.qualifyUrl(url));

        promise.catch((errorThrown) => {
            if (errorThrown.additional.status !== 422) {
                UserNotification.error("Loading keyword preview failed with status: " + errorThrown,
                    "Could not load keyword preview");
            }
        });

        return promise;
    },
    testRegex(regex: string, string: string): Promise<Object> {
        const url = jsRoutes.controllers.api.ToolsApiController.regexTest().url;
        const promise = fetch('POST', URLUtils.qualifyUrl(url), {regex: regex, string: string});

        promise.catch((errorThrown) => {
            UserNotification.error("Testing regular expression failed with status: " + errorThrown,
                "Could not test regular expression");
        });

        return promise;
    }
};

export = ToolsStore;
