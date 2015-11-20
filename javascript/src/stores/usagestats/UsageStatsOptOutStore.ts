/// <reference path="../../../declarations/bluebird/bluebird.d.ts" />

import jsRoutes = require('routing/jsRoutes');
import URLUtils = require("../../util/URLUtils");
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
            .catch(error => {
                if (error.additional.status === 404) {
                    console.log('Usage stats configuration does not exist. Plugin not loaded?');
                } else {
                    console.log('Unable to load usage stats configuration', error);
                }

                return false;
            });

        return promise;
    },
    getOptOutState(): Promise<UsageStatsOptOutState> {
        var url = URLUtils.qualifyUrl(jsRoutes.controllers.api.UsageStatsApiController.setOptOutState().url);
        var promise = fetch('GET', url);

        promise = promise.catch((error) => {
            if (error.additional.status === 404) {
                console.log('Opt-out state does not exist. Plugin not loaded?');
            } else {
                UserNotification.error("Loading usage stats opt-out state failed: " + error);
            }

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
