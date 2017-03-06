/// <reference path="../../../declarations/bluebird/bluebird.d.ts" />

const Reflux = require('reflux');

const UserNotification = require('util/UserNotification');
import ApiRoutes = require('routing/ApiRoutes');
const URLUtils = require('util/URLUtils');
const fetchPeriodically = require('logic/rest/FetchProvider').fetchPeriodically;
const fetch = require('logic/rest/FetchProvider').default;

const ActionsProvider = require('injection/ActionsProvider');
const WidgetsActions = ActionsProvider.getActions('Widgets');

interface Widget {
    id: string;
    description: string;
    type: string;
    cache_time: number;
    creator_user_id?: string;
    config: {};
}

const WidgetsStore = Reflux.createStore({
    listenables: [WidgetsActions],
    _serializeWidgetForUpdate(widget: Widget): any {
        return {
            description: widget.description,
            type: widget.type,
            cache_time: widget.cache_time,
            creator_user_id: widget.creator_user_id,
            config: widget.config,
        };
    },

    addWidget(dashboardId: string, widgetType: string, widgetTitle: string, widgetConfig: Object): Promise<string[]> {
        var widgetData = {description: widgetTitle, type: widgetType, config: widgetConfig};
        var url = URLUtils.qualifyUrl(ApiRoutes.DashboardsApiController.addWidget(dashboardId).url);
        var promise = fetch('POST', url, widgetData);

        promise.then(
          response => {
              UserNotification.success("Widget created successfully");
              return response;
          },
          error => {
              if (error.additional.status !== 404) {
                  UserNotification.error("Creating widget failed with status: " + error,
                    "Could not create widget");
              }
          });

        return promise;
    },

    loadWidget(dashboardId: string, widgetId: string): Promise<string[]> {
        var url = URLUtils.qualifyUrl(ApiRoutes.DashboardsApiController.widget(dashboardId, widgetId).url);
        const promise = fetchPeriodically('GET', url);

        promise.catch((error) => {
            if (error.additional.status !== 404) {
                UserNotification.error("Loading widget information failed with status: " + error,
                    "Could not load widget information");
            }
        });
        return promise;
    },

    updateWidget(dashboardId: string, widget: Widget): Promise<string[]> {
        var url = URLUtils.qualifyUrl(ApiRoutes.DashboardsApiController.updateWidget(dashboardId, widget.id).url);
        var promise = fetch('PUT', url, this._serializeWidgetForUpdate(widget));

        promise.then(
          response => {
              UserNotification.success("Widget updated successfully");
              return response;
          },
          error => {
              UserNotification.error("Updating widget \"" + widget.description + "\" failed with status: " + error.message,
                "Could not update widget");
          }
        );

        return promise;
    },

    loadValue(dashboardId: string, widgetId: string, resolution: number): Promise<string[]> {
        var url = URLUtils.qualifyUrl(ApiRoutes.DashboardsApiController.widgetValue(dashboardId, widgetId, resolution).url);

        return fetchPeriodically('GET', url);
    },

    removeWidget(dashboardId: string, widgetId: string): Promise<string[]> {
        const url = URLUtils.qualifyUrl(ApiRoutes.DashboardsApiController.removeWidget(dashboardId, widgetId).url);

        const promise = fetch('DELETE', url).then(response => {
            this.trigger({delete: widgetId});
            return response;
        });
        WidgetsActions.removeWidget.promise(promise);

        return promise;
    },
});

module.exports = WidgetsStore;
