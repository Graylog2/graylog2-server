/// <reference path="../../../declarations/jquery/jquery.d.ts" />

'use strict';

import UserNotification = require("../../util/UserNotification");
import URLUtils = require("../../util/URLUtils");

var FieldsStore = {
    URL: URLUtils.appPrefixed('/a/system/fields'),
    loadFields(): JQueryPromise<any> {
        var promise = $.getJSON(this.URL);
        promise.fail((jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Loading field information failed with status: " + errorThrown,
                "Could not load field information");
        });
        return promise.then((data) => data.fields);
    }
};
export = FieldsStore;
