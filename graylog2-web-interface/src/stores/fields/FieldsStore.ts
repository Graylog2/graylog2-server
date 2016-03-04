/// <reference path="../../../declarations/bluebird/bluebird.d.ts" />

import jsRoutes = require('routing/jsRoutes');
const fetch = require('logic/rest/FetchProvider').default;
import Immutable = require('immutable');
const UserNotification = require("util/UserNotification");
const URLUtils = require('util/URLUtils');

var FieldsStore = {
    loadFields(): Promise<string[]> {
        const url = jsRoutes.SystemApiController.fields().url;
        let promise = fetch('GET', URLUtils.qualifyUrl(url));
        promise = promise.then((data) => data.fields);
        promise.catch((errorThrown) => {
            UserNotification.error("Loading field information failed with status: " + errorThrown.additional.message,
                "Could not load field information");
        });
        return promise;
    }
};
export = FieldsStore;
