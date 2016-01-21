/// <reference path="../../../declarations/bluebird/bluebird.d.ts" />

import jsRoutes = require('routing/jsRoutes');
const URLUtils = require('util/URLUtils');
const fetch = require('logic/rest/FetchProvider').default;

import UserNotification = require("../../util/UserNotification");

export interface UsageStatsOptOutState {
    opt_out: boolean
}

export var UsageStatsOptOutStore = {
    pluginEnabled(): Promise<boolean> {
        var url = URLUtils.qualifyUrl(jsRoutes.controllers.api.UsageStatsApiController.pluginEnabled().url);
        var promise = fetch('GET', url);

        promise = promise
            .then(response => {
                return response.enabled;
            })
            .catch(() => {
                // When the plugin is not loaded the CORS options request will fail and we can't tell at this point
                // what was the cause for the problem. Therefore, we return false and don't notify the user.
                return false;
            });

        return promise;
    },
    getOptOutState(): Promise<UsageStatsOptOutState> {
        var url = URLUtils.qualifyUrl(jsRoutes.controllers.api.UsageStatsApiController.setOptOutState().url);
        var promise = fetch('GET', url);

        promise = promise.catch(() => {
            // When the plugin is not loaded the CORS options request will fail and we can't tell at this point
            // what was the cause for the problem. Therefore, we return false and don't notify the user.
            return null;
        });

        return promise;
    },
    setOptOut(notify: boolean): Promise<boolean> {
        return this._sendOptOutState({opt_out: true}, () => {
            if (notify === true) {
                UserNotification.success("No anonymous usage stats will be sent.", "Opt-out created");
            }
        }, (error) => {
            UserNotification.error("Please try again",
                "Setting anonymous usage stats opt-out failed: " + error);
        });
    },
    setOptIn(notify: boolean): Promise<boolean> {
        return this._sendOptOutState({opt_out: false}, () => {
            if (notify === true) {
                UserNotification.success("Thank you for helping us making Graylog better!");
            }
        }, (error) => {
            UserNotification.error("Please try again",
                "Opt-in failed: " + error);
        });
    },
    _sendOptOutState(optOutState: UsageStatsOptOutState, success: Function, error: Function): Promise<boolean> {
        var url = URLUtils.qualifyUrl(jsRoutes.controllers.api.UsageStatsApiController.setOptOutState().url);
        var promise = fetch('POST', url, JSON.stringify(optOutState));

        promise
            .then(() => success())
            .catch((error) => error(error));

        return promise;
    }
};
