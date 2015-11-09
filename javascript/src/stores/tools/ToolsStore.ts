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
            UserNotification.error('Details: ' + errorThrown,
                'Could not try regular expression. Make sure that it is valid.');
        });

        return promise;
    },
    testGrok(pattern: string, string: string): Promise<Object> {
        const url = jsRoutes.controllers.api.ToolsApiController.grokTest().url;
        const promise = fetch('POST', URLUtils.qualifyUrl(url), {pattern: pattern, string: string});

        promise.catch((errorThrown) => {
            UserNotification.error('Details: ' + errorThrown,
                'We were not able to run the grok extraction. Please check your parameters.');
        });

        return promise;
    }
};

export = ToolsStore;
