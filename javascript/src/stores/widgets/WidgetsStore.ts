/// <reference path="../../../declarations/jquery/jquery.d.ts" />

'use strict';

import UserNotification = require("../../util/UserNotification");
import URLUtils = require("../../util/URLUtils");

interface SerializedWidget {
    id: string;
    description: string;
    type: string;
    cache_time: number;
    creator_user_id?: string;
    config: {};
}

interface Widget {
    id: string;
    title: string;
    type: string;
    cacheTime: number;
    creatorUserId?: string;
    config: {};
}

var WidgetsStore = {
    _deserializeWidget(widget: SerializedWidget): Widget {
        return {
            id: widget.id,
            title: widget.description,
            type: widget.type,
            cacheTime: widget.cache_time,
            creatorUserId: widget.creator_user_id,
            config: widget.config
        };
    },
    _serializeWidget(widget: Widget): SerializedWidget {
        return {
            id: widget.id,
            description: widget.title,
            type: widget.type,
            cache_time: widget.cacheTime,
            creator_user_id: widget.creatorUserId,
            config: widget.config
        };
    },

    loadWidget(dashboardId: string, widgetId: string): JQueryPromise<string[]> {
        var url = URLUtils.appPrefixed('/a/dashboards/' + dashboardId + '/widgets/' + widgetId);
        var promise = $.getJSON(url);
        promise.fail((jqXHR, textStatus, errorThrown) => {
            if (jqXHR.status !== 404) {
                UserNotification.error("Loading widget information failed with status: " + errorThrown,
                    "Could not load widget information");
            }
        });
        return promise.then((widget) => this._deserializeWidget(widget));
    },

    updateWidget(dashboardId: string, widget: Widget) {
        var url = URLUtils.appPrefixed('/a/dashboards/' + dashboardId + '/widgets/' + widget.id);
        console.log(url);
        console.log(JSON.stringify(this._serializeWidget(widget)));
        var promise = $.ajax({
            type: "PUT",
            url: url,
            data: JSON.stringify(this._serializeWidget(widget)),
            dataType: 'json',
            contentType: 'application/json'
        });

        promise.done(() => UserNotification.success("Widget updated successfully"));
        promise.fail((jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Updating widget '" + widget.title + "' failed with status: " + errorThrown,
                "Could not update widget");
        });

        return promise;
    },

    loadValue(dashboardId: string, widgetId: string, resolution: number): JQueryPromise<string[]> {
        var url = URLUtils.appPrefixed('/a/dashboards/' + dashboardId + '/widgets/' + widgetId + '/resolution/' + resolution + '/value');
        return $.getJSON(url);
    }
};

export = WidgetsStore;