/// <reference path="../../../declarations/jquery/jquery.d.ts" />

'use strict';

declare
var jsRoutes: any;

import UserNotification = require("../../util/UserNotification");

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

    addWidget(dashboardId: string, widgetType: string, widgetTitle: string, widgetConfig: Object): JQueryPromise<string[]> {
        var widgetData = {description: widgetTitle, type: widgetType, config: widgetConfig};
        var url = jsRoutes.controllers.api.DashboardsApiController.addWidget(dashboardId).url;
        var promise = $.ajax({
            type: "POST",
            url: url,
            data: JSON.stringify(widgetData),
            dataType: 'json',
            contentType: 'application/json'
        });

        promise.done(() => UserNotification.success("Widget created successfully"));
        promise.fail((jqXHR, textStatus, errorThrown) => {
            if (jqXHR.status !== 404) {
                UserNotification.error("Creating widget failed with status: " + errorThrown,
                    "Could not create widget");
            }
        });

        return promise;
    },

    loadWidget(dashboardId: string, widgetId: string): JQueryPromise<string[]> {
        var url = jsRoutes.controllers.api.DashboardsApiController.widget(dashboardId, widgetId).url;
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
        var url = jsRoutes.controllers.api.DashboardsApiController.updateWidget(dashboardId, widget.id).url;
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
        var url = jsRoutes.controllers.api.DashboardsApiController.widgetValue(dashboardId, widgetId, resolution).url;
        return $.getJSON(url);
    }
};

export = WidgetsStore;