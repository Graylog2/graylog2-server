import Reflux from 'reflux';
import Immutable from 'immutable';

import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';

const ViewActions = Reflux.createActions({
  get: { asyncResult: true },
  save: { asyncResult: true },
  search: { asyncResult: true },
  delete: { asyncResult: true },
});

const mutateWidgets = (widgets, widgetFilters) => {
  return widgets.map((widget) => {
    const newWidgetConfig = {};
    const config = widget.config;
    Object.keys(config).forEach((widgetConfigKey) => {
      const newWidgetConfigKey = widgetConfigKey.replace(/([A-Z])/g, (_, wordStart) => `_${wordStart.toLowerCase()}`);
      newWidgetConfig[newWidgetConfigKey] = config[widgetConfigKey];
    });
    const cleanedWidget = widget.delete('title')
      .delete('computationTimeRange')
      .update('type', type => type.toLowerCase());
    const filter = widgetFilters.get(widget.id);
    const finalWidget = filter ? cleanedWidget.set('filter', filter) : cleanedWidget;
    return Object.assign({}, finalWidget.toJS(), { config: newWidgetConfig });
  });
};

const prepareDashboardWidgets = (widgets, positions) => {
  if (!widgets) {
    return null;
  }

  const newWidgets = {};

  widgets.valueSeq().forEach((widget) => {
    newWidgets[widget.widgetId] = {
      query_id: widget.queryId,
      widget_id: widget.widgetId,
    };
  });

  return { widgets: newWidgets, positions: positions };
};

const _prepareViewRequest = (id, currentViewStore, view, widgets, dashboardWidgets, fields, search, titles, widgetFilters) => {
  const { positions, dashboardPositions, title, summary, description } = view.toJS();
  const { widgetMapping } = search.result.searchRequest;
  const { search_id } = search.result.result;
  const state = widgets.map((queryWidgets, queryId) => {
    return {
      widgets: queryWidgets.valueSeq(),
      positions: positions[queryId] || {},
      widget_mapping: widgetMapping.filter((_, widgetId) => queryWidgets.has(widgetId)).toJS(),
      selected_fields: fields.get(queryId, Immutable.List()).toJS(),
      titles: titles.get(queryId, new Immutable.Map()),
    };
  }).toJS();
  const dashboardState = prepareDashboardWidgets(dashboardWidgets.get(view.get('id')) || new Immutable.Map(), dashboardPositions || {});

  return {
    id,
    title,
    summary,
    description,
    state,
    dashboard_state: dashboardState,
    search_id,
  };
};

const viewsUrl = URLUtils.qualifyUrl('/plugins/org.graylog.plugins.enterprise/views');
const viewsIdUrl = id => URLUtils.qualifyUrl(`/plugins/org.graylog.plugins.enterprise/views/${id}`);

const ViewStore = Reflux.createStore({
  listenables: [ViewActions],

  views: undefined,
  pagination: {
    total: 0,
    count: 0,
    page: 1,
    perPage: 10,
  },

  getInitialState() {
    return {
      pagination: this.pagination,
      list: this.views,
    };
  },

  get(viewId) {
    const promise = fetch('GET', `${viewsUrl}/${viewId}`);
    ViewActions.get.promise(promise);
  },

  save(id, currentViewStore, view, widgets, dashboardWidgets, fields, search, titles, widgetFilters) {
    const request = _prepareViewRequest(id, currentViewStore, view, widgets, dashboardWidgets, fields, search, titles, widgetFilters);
    const promise = fetch('POST', viewsUrl, request);
    ViewActions.save.promise(promise);
  },

  search(query, page = 1, perPage = 10, sortBy = 'title', order = 'asc') {
    const promise = fetch('GET', `${viewsUrl}?query=${query}&page=${page}&per_page=${perPage}&sort=${sortBy}&order=${order}`)
      .then((response) => {
        this.views = response.views;
        this.pagination = {
          total: response.total,
          count: response.count,
          page: response.page,
          perPage: response.per_page,
        };
        this.trigger({
          list: this.views,
          pagination: this.pagination,
        });

        return response;
      })
      .catch((error) => {
        UserNotification.error(`Fetching views failed with status: ${error}`,
          'Could not retrieve views');
      });

    ViewActions.search.promise(promise);
  },

  delete(view) {
    const promise = fetch('DELETE', viewsIdUrl(view.id)).catch((error) => {
      UserNotification.error(`Deleting view ${view.title} failed with status: ${error}`,
        'Could not delete view');
    });

    ViewActions.delete.promise(promise);
  },
});

export { ViewStore, ViewActions };
