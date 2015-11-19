const Reflux = require('reflux');

import UserNotification = require('util/UserNotification');
import jsRoutes = require('routing/jsRoutes');
import URLUtils = require('util/URLUtils');
const Builder = require('logic/rest/FetchProvider').Builder;
const fetch = require('logic/rest/FetchProvider').default;
const WidgetsActions = require('actions/widgets/WidgetsActions');

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

const WidgetsStore = Reflux.createStore({
    listenables: [WidgetsActions],
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
    _serializeWidgetForUpdate(widget: Widget): any {
        return {
            description: widget.title,
            type: widget.type,
            cache_time: widget.cacheTime,
            creator_user_id: widget.creatorUserId,
            config: widget.config,
        };
    },

    addWidget(dashboardId: string, widgetType: string, widgetTitle: string, widgetConfig: Object): JQueryPromise<string[]> {
        var widgetData = {description: widgetTitle, type: widgetType, config: widgetConfig};
        var url = URLUtils.qualifyUrl(jsRoutes.controllers.api.DashboardsApiController.addWidget(dashboardId).url);
        var promise = fetch('POST', url, widgetData);

        promise.then(() => UserNotification.success("Widget created successfully"),
        (jqXHR, textStatus, errorThrown) => {
            if (jqXHR.status !== 404) {
                UserNotification.error("Creating widget failed with status: " + errorThrown,
                    "Could not create widget");
            }
        });

        return promise;
    },

    loadWidget(dashboardId: string, widgetId: string): JQueryPromise<string[]> {
        var url = URLUtils.qualifyUrl(jsRoutes.controllers.api.DashboardsApiController.widget(dashboardId, widgetId).url);
        const promise = new Builder('GET', url)
            .authenticated()
            .setHeader('X-Graylog2-No-Session-Extension', 'true')
            .json()
            .build();

        promise.catch((jqXHR, textStatus, errorThrown) => {
            if (jqXHR.status !== 404) {
                UserNotification.error("Loading widget information failed with status: " + errorThrown,
                    "Could not load widget information");
            }
        });
        return promise.then((widget) => this._deserializeWidget(widget));
    },

    updateWidget(dashboardId: string, widget: Widget) {
        var url = URLUtils.qualifyUrl(jsRoutes.controllers.api.DashboardsApiController.updateWidget(dashboardId, widget.id).url);
        var promise = fetch('PUT', url, this._serializeWidgetForUpdate(widget));

        promise.then(() => UserNotification.success("Widget updated successfully"),
        (error) => {
            UserNotification.error("Updating widget \"" + widget.title + "\" failed with status: " + error.message,
                "Could not update widget");
        });

        return promise;
    },

    loadValue(dashboardId: string, widgetId: string, resolution: number): JQueryPromise<string[]> {
        var url = URLUtils.qualifyUrl(jsRoutes.controllers.api.DashboardsApiController.widgetValue(dashboardId, widgetId, resolution).url);

        return new Builder('GET', url)
            .authenticated()
            .setHeader('X-Graylog2-No-Session-Extension', 'true')
            .json()
            .build();
    },

    removeWidget(dashboardId: string, widgetId: string): void {
        const url = URLUtils.qualifyUrl(jsRoutes.controllers.api.DashboardsApiController.removeWidget(dashboardId, widgetId).url);

        const promise = fetch('DELETE', url).then(() => {
            this.trigger({delete: widgetId});
        });
        WidgetsActions.removeWidget.promise(promise);

        return promise;
    },
});

export default WidgetsStore;
