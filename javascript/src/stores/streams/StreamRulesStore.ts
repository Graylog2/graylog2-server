'use strict';

declare var jsRoutes: any;
declare var $: any;

import UserNotification = require("../../util/UserNotification");
import URLUtils = require("../../util/URLUtils");

interface StreamRuleType {
    id: number;
    short_desc: string;
    long_desc: string;
}

interface StreamRule {
    field: string;
    type: number;
    value: string;
    inverted: boolean;
}

interface Callback {
    (): void;
}

class StreamRulesStore {
    private callbacks: Array<Callback> = [];

    types(callback: ((streamRuleTypes: Array<StreamRuleType>) => void)) {
        var failCallback = (jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Fetching Stream Rule Types failed with status: " + errorThrown,
                "Could not retrieve Stream Rule Types!");
        };

        $.getJSON(jsRoutes.controllers.api.StreamRulesApiController.types().url, callback).fail(failCallback);
    }
    list(streamId: string, callback: ((streamRules: Array<StreamRule>) => void)) {
        var failCallback = (jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Fetching Stream Rules failed with status: " + errorThrown,
                "Could not retrieve Stream Rules!");
        };

        $.getJSON(jsRoutes.controllers.api.StreamRulesApiController.list(streamId).url, callback).fail(failCallback);
    }
    update(streamId: string, streamRuleId: string, data: StreamRule, callback: (() => void)) {
        var failCallback = (jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Updating Stream Rule failed with status: " + errorThrown,
                "Could not update Stream Rule!");
        };

        var url = jsRoutes.controllers.api.StreamRulesApiController.update(streamId, streamRuleId).url;
        var request = {field: data.field, type: data.type, value: data.value, inverted: data.inverted};

        $.ajax({
            type: "PUT",
            url: url,
            contentType: "application/json",
            data: JSON.stringify(request)
        }).done(callback).done(this._emitChange.bind(this)()).fail(failCallback);
    }
    remove(streamId: string, streamRuleId: string, callback: (() => void)) {
        var failCallback = (jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Deleting Stream Rule failed with status: " + errorThrown,
                "Could not delete Stream Rule!");
        };

        var url = jsRoutes.controllers.api.StreamRulesApiController.delete(streamId, streamRuleId).url;
        $.ajax({
            type: "DELETE",
            url: url
        }).done(callback).done(this._emitChange.bind(this)).fail(failCallback);
    }
    create(streamId: string, data: StreamRule, callback: (() => void)) {
        var failCallback = (jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Creating Stream Rule failed with status: " + errorThrown,
                "Could not create Stream Rule!");
        };

        var url = jsRoutes.controllers.api.StreamRulesApiController.create(streamId).url;

        $.ajax({
            type: "POST",
            url: url,
            contentType: "application/json",
            data: JSON.stringify(data)
        }).done(callback).done(this._emitChange.bind(this)).fail(failCallback);
    }
    onChange(callback) {
        this.callbacks.push(callback);
    }
    _emitChange() {
        this.callbacks.forEach((callback) => callback());
    }
}

var streamRulesStore = new StreamRulesStore();

export = streamRulesStore;
