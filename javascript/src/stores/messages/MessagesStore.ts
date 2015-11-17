/// <reference path="../../../declarations/bluebird/bluebird.d.ts" />

import jsRoutes = require('routing/jsRoutes');
const fetch = require('logic/rest/FetchProvider').default;
import UserNotification = require("util/UserNotification");
import URLUtils = require("util/URLUtils");

interface Field {
    name: string;
    value: string;
}

interface Message {
    id: string;
    index: number;
    fields: Array<Field>;
}

var MessagesStore = {
    loadMessage(index: string, messageId: string): Promise<Message> {
        var url = jsRoutes.controllers.MessagesController.single(index.trim(), messageId.trim()).url;
        const promise = fetch('GET', URLUtils.qualifyUrl(url))
            .then(response => {
              return {
                id: messageId,
                fields: response.message,
                index: response.index,
              };
            })
            .catch(errorThrown => {
                UserNotification.error("Loading message information failed with status: " + errorThrown,
                    "Could not load message information");
            });
        return promise;
    },

    fieldTerms(index: string, messageId: string, field: string): JQueryPromise<Array<string>> {
        var url = jsRoutes.controllers.MessagesController.analyze(index, messageId, field).url;
        var promise = $.getJSON(url);
        promise.fail((jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Loading field terms failed with status: " + errorThrown,
                "Could not load field terms.");
        });
        return promise;
    }
};

export = MessagesStore;
