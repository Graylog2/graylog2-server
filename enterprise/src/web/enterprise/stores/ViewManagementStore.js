import Reflux from 'reflux';

import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';

const ViewActions = Reflux.createActions({
  get: { asyncResult: true },
  save: { asyncResult: true },
  search: { asyncResult: true },
  delete: { asyncResult: true },
});

const mutateWidgets = (widgets) => {
  return widgets.map((widget) => {
    const newWidgetConfig = {};
    const config = widget.get('config');
    Object.keys(config).forEach((widgetConfigKey) => {
      const newWidgetConfigKey = widgetConfigKey.replace(/([A-Z])/g, (_, wordStart) => `_${wordStart.toLowerCase()}`);
      newWidgetConfig[newWidgetConfigKey] = config[widgetConfigKey];
    });
    const cleanedWidget = widget.delete('title')
      .delete('computationTimeRange')
      .update('type', type => type.toLowerCase());
    return Object.assign({}, cleanedWidget.toJS(), { config: newWidgetConfig });
  });
};

const _prepareViewRequest = (id, title, currentViewStore, view, widgets, fields, search) => {
  const { positions } = view.toJS();
  const { widgetMapping } = search.result.searchRequest;
  const { search_id } = search.result.result;
  const state = widgets.map((queryWidgets, queryId) => {
    return {
      widgets: mutateWidgets(queryWidgets.valueSeq()).toJS(),
      positions: positions[queryId] || {},
      widget_mapping: widgetMapping.filter((_, widgetId) => queryWidgets.has(widgetId)).toJS(),
      selected_fields: fields.get(queryId).toJS(),
    };
  }).toJS();

  return {
    id,
    title,
    state,
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

  save(id, title, currentViewStore, view, widgets, fields, search) {
    const request = _prepareViewRequest(id, title, currentViewStore, view, widgets, fields, search);
    const promise = fetch('POST', viewsUrl, request).then(response => console.log(response));
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
