/// <reference path="../../../declarations/bluebird/bluebird.d.ts" />

import UserNotification = require("../../util/UserNotification");
import URLUtils = require("../../util/URLUtils");

import jsRoutes = require('routing/jsRoutes');
const fetch = require('logic/rest/FetchProvider').default;

interface Input {
    id: string;
    title: string;
    description: string;
    creatorUser: string;
    createdAt: number;
}

var InputsStore = {
    list(callback: ((inputs: Array<Input>) => void), filtered: boolean = true) {
        var failCallback = (errorThrown) => {
            UserNotification.error("Fetching Inputs failed with status: " + errorThrown,
                "Could not retrieve Inputs");
        };

        fetch('GET', URLUtils.qualifyUrl(jsRoutes.controllers.api.InputsApiController.list().url))
            .then(response => {
                const inputs = (filtered ? response.inputs.map((input) => input.message_input) : response.inputs);
                callback(inputs);
            }, failCallback);
    },
    get(inputId: string): Promise<Input> {
        const promise = fetch('GET', URLUtils.qualifyUrl(jsRoutes.controllers.api.InputsApiController.get(inputId).url));

        promise.catch(errorThrown => {
            UserNotification.error(`Fetching input ${inputId} failed with status: ${errorThrown}`,
                "Could not retrieve input");
        });

        return promise;
    },
    globalRecentMessage(input: any, callback: ((message: any) => void)) {
        var failCallback = (errorThrown) => {
            UserNotification.error("Loading recent message failed with status: " + errorThrown,
                "Could not retrieve recent message from input \"" + input.title + "\"");
        };

        fetch('GET', URLUtils.qualifyUrl(jsRoutes.controllers.api.InputsApiController.globalRecentMessage(input.input_id).url))
            .then(callback, failCallback);
    }
};

export = InputsStore;
