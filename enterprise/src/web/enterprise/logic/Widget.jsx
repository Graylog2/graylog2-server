import { PluginStore } from 'graylog-web-plugin/plugin';

const widgetsKey = 'enterpriseWidgets';

export function widgetDefinition(type) {
  return PluginStore.exports(widgetsKey)
    .find(widget => widget.type.toLocaleUpperCase() === type.toLocaleUpperCase())
}

export const messageList = (id, timeRange, fields) => {
  return {
    id: id,
    type: 'messages',
    title: 'Messages',
    config: {
      fields: fields,
      showMessageRow: true,
    },
    computationTimeRange: timeRange,
  };
};

export const resultHistogram = (id, timeRange = {}) => {
  return {
    id: id,
    type: 'SEARCH_RESULT_CHART2',
    title: 'Histogram',
    computationTimeRange: timeRange,
    config: {
      timerange: timeRange,
    },
  };
};

export const searchSideBar = () => {
  return {
    type: 'SEARCH_SIDEBAR',
    title: 'Search Result',
    config: {},
    data: 'messages',
  };
};

export const dataTable = (id) => {
  return {
    id,
    type: 'DATATABLE',
    title: 'Results',
    config: {
      fields: ['action', 'controller', 'count'],
      data: [
        { action: 'index', controller: 'PostsController', count: 19016 },
        { controller: 'UsersController', count: 1376 },
        { action: 'login', controller: 'LoginController', count: 5156 },
        { action: 'show', controller: 'PostsController', count: 5903 },
        { action: 'edit', controller: 'PostsController', count: 922 },
      ],
    },
  };
};

const createWidgetDefinitions = (timeRange, fields) => {
  return {
    data: {},
    widgets: {
      messages: messageList(timeRange, fields),
      histogram: resultHistogram(timeRange),
      searchSideBar: searchSideBar(),
    },
    positions: {
      searchSideBar: { row: 1, col: 1, width: 1, height: 3.2 },
      histogram: { row: 1, col: 2, width: 4, height: 1.2 },
      messages: { row: 255, col: 2, width: 4, height: 3 },
    },
  };
};

