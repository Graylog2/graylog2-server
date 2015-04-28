'use strict';

declare var jsRoutes: any;
declare var $: any;

import UserNotification = require("../../util/UserNotification");
import URLUtils = require("../../util/URLUtils");

var StreamsStore = {
    load(callback: ((streams: any) => void)) {
        var failCallback = (jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Fetching Streams failed with status: " + errorThrown,
                "Could not retrieve Streams!");
        };

        $.getJSON(jsRoutes.controllers.api.StreamsApiController.list().url, callback).fail(failCallback);
    },
    remove(streamId: string, callback: (() => void)) {
        var failCallback = (jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Removing Stream failed with status: " + errorThrown,
                "Could not remove Stream!");
        };

        var url = jsRoutes.controllers.api.StreamsApiController.delete(streamId).url;
        $.ajax({
            type: "DELETE",
            url: url
        }).done(callback).fail(failCallback);
    },
    resume(streamId: string, callback: (() => void)) {
        var failCallback = (jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Resuming Stream failed with status: " + errorThrown,
                "Could not resume Stream!");
        };

        var url = jsRoutes.controllers.api.StreamsApiController.resume(streamId).url;
        $.ajax({
            type: "POST",
            url: url
        }).done(callback).fail(failCallback);
    },
    save(stream: any, callback: ((streamId: string) => void)) {
        var failCallback = (jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Saving Stream failed with status: " + errorThrown,
                "Could not save Stream!");
        };

        var url = jsRoutes.controllers.api.StreamsApiController.create().url;
        $.ajax({
            type: "POST",
            url: url,
            contentType: "application/json",
            data: JSON.stringify(stream)
        }).done(callback).fail(failCallback);
    },
    update(streamId: string, data: any, callback: (() => void)) {
        var failCallback = (jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Updating Stream failed with status: " + errorThrown,
                "Could not update Stream!");
        };

        var url = jsRoutes.controllers.api.StreamsApiController.update(streamId).url;
        $.ajax({
            type: "PUT",
            url: url,
            contentType: "application/json",
            data: JSON.stringify(data)
        }).done(callback).fail(failCallback);
    },
    cloneStream(streamId: string, data: any, callback: (() => void)) {
        var failCallback = (jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Cloning Stream failed with status: " + errorThrown,
                "Could not clone Stream!");
        };

        var url = jsRoutes.controllers.api.StreamsApiController.cloneStream(streamId).url;
        $.ajax({
            type: "POST",
            url: url,
            contentType: "application/json",
            data: JSON.stringify(data)
        }).done(callback).fail(failCallback);
    }
};

export = StreamsStore;
