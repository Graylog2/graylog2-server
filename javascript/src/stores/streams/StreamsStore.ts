'use strict';

declare var jsRoutes: any;
declare var $: any;

import UserNotification = require("../../util/UserNotification");
import URLUtils = require("../../util/URLUtils");

interface Stream {
    id: string;
    title: string;
    description: string;
    creatorUser: string;
    createdAt: number;
}

var StreamsStore = {
    load(callback: ((streams: Array<Stream>) => void)) {
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
    pause(streamId: string, callback: (() => void)) {
        var failCallback = (jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Pausing Stream failed with status: " + errorThrown,
                "Could not resume Stream!");
        };

        var url = jsRoutes.controllers.api.StreamsApiController.pause(streamId).url;
        $.ajax({
            type: "POST",
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
    },
    removeOutput(streamId: string, outputId: string, callback: (jqXHR, textStatus, errorThrown) => void) {
        $.ajax({
            url: jsRoutes.controllers.api.StreamOutputsApiController.delete(streamId, outputId).url,
            type: 'DELETE',
            error: (jqXHR, textStatus, errorThrown) => {
                UserNotification.error("Removing output from stream failed with status: " + errorThrown,
                    "Could not remove output from stream");
            },
            success: callback
        });
    },
    addOutput(streamId: string, outputId: string, callback: (jqXHR, textStatus, errorThrown) => void) {
        $.ajax({
            url: jsRoutes.controllers.api.StreamOutputsApiController.delete(streamId, outputId).url,
            type: 'PUT',
            error: (jqXHR, textStatus, errorThrown) => {
                UserNotification.error("Adding output to stream failed with status: " + errorThrown,
                    "Could not add output to stream");
            },
            success: callback
        });
    }
};

export = StreamsStore;
