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

interface TestMatchResponse {
    matches: boolean;
    rules: any;
}

interface Callback {
    (): void;
}

class StreamsStore {
    private callbacks: Array<Callback> = [];

    listStreams() {
        var url = jsRoutes.controllers.api.StreamsApiController.list().url;
        var promise = $.getJSON(url);
        promise.fail((jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Loading streams failed with status: " + errorThrown,
                "Could not load streams.");
        });
        return promise;
    }
    load(callback: ((streams: Array<Stream>) => void)) {
        var failCallback = (jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Fetching Streams failed with status: " + errorThrown,
                "Could not retrieve Streams!");
        };

        this.listStreams().done(callback).fail(failCallback);
    }
    get(streamId: string, callback: ((stream: Stream) => void)) {
        var failCallback = (jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Loading Stream failed with status: " + errorThrown,
                "Could not retrieve Stream!");
        };

        var url = jsRoutes.controllers.api.StreamsApiController.get(streamId).url;
        $.getJSON(url).done(callback).fail(failCallback);
    }
    remove(streamId: string, callback: (() => void)) {
        var failCallback = (jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Removing Stream failed with status: " + errorThrown,
                "Could not remove Stream!");
        };

        var url = jsRoutes.controllers.api.StreamsApiController.delete(streamId).url;
        $.ajax({
            type: "DELETE",
            url: url
        }).done(callback).done(this._emitChange.bind(this)).fail(failCallback);
    }
    pause(streamId: string, callback: (() => void)) {
        var failCallback = (jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Pausing Stream failed with status: " + errorThrown,
                "Could not pause Stream!");
        };

        var url = jsRoutes.controllers.api.StreamsApiController.pause(streamId).url;
        $.ajax({
            type: "POST",
            url: url
        }).done(callback).done(this._emitChange.bind(this)).fail(failCallback);
    }
    resume(streamId: string, callback: (() => void)) {
        var failCallback = (jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Resuming Stream failed with status: " + errorThrown,
                "Could not resume Stream!");
        };

        var url = jsRoutes.controllers.api.StreamsApiController.resume(streamId).url;
        $.ajax({
            type: "POST",
            url: url
        }).done(callback).done(this._emitChange.bind(this)).fail(failCallback);
    }
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
        }).done(callback).done(this._emitChange.bind(this)).fail(failCallback);
    }
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
        }).done(callback).done(this._emitChange.bind(this)).fail(failCallback);
    }
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
        }).done(callback).done(this._emitChange.bind(this)).fail(failCallback);
    }
    removeOutput(streamId: string, outputId: string, callback: (jqXHR, textStatus, errorThrown) => void) {
        $.ajax({
            url: jsRoutes.controllers.api.StreamOutputsApiController.delete(streamId, outputId).url,
            type: 'DELETE',
            error: (jqXHR, textStatus, errorThrown) => {
                UserNotification.error("Removing output from stream failed with status: " + errorThrown,
                    "Could not remove output from stream");
            }
        }).done(callback).done(this._emitChange.bind(this));
    }
    addOutput(streamId: string, outputId: string, callback: (jqXHR, textStatus, errorThrown) => void) {
        $.ajax({
            url: jsRoutes.controllers.api.StreamOutputsApiController.delete(streamId, outputId).url,
            type: 'PUT',
            error: (jqXHR, textStatus, errorThrown) => {
                UserNotification.error("Adding output to stream failed with status: " + errorThrown,
                    "Could not add output to stream");
            }
        }).done(callback).done(this._emitChange.bind(this));
    }
    testMatch(streamId: string, message: any, callback: (response: TestMatchResponse) => void) {
        var config = {
            url: jsRoutes.controllers.api.StreamsApiController.testMatch(streamId).url,
            type: 'POST',
            contentType: "application/json",
            data: JSON.stringify(message),
            error: (jqXHR, textStatus, errorThrown) => {
                UserNotification.error("Testing stream rules of stream failed with status: " + errorThrown,
                    "Could not test stream rules of stream");
            },
            success: callback
        };
        $.ajax(config);
    }
    onChange(callback: () => void) {
        this.callbacks.push(callback);
    }
    _emitChange() {
        this.callbacks.forEach((callback) => callback());
    }
}

var streamsStore = new StreamsStore();
export = streamsStore;

