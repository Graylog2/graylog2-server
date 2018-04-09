import { PluginStore } from 'graylog-web-plugin/plugin';

const widgetsKey = 'enterpriseWidgets';

export function widgetDefinition(type) {
  return PluginStore.exports(widgetsKey)
    .find(widget => widget.type.toLocaleUpperCase() === type.toLocaleUpperCase())
}

export const messageList = (id, fields = []) => {
  return {
    id: id,
    type: 'messages',
    config: {
      fields: fields,
      showMessageRow: true,
    },
  };
};

export const resultHistogram = (id) => {
  return {
    id: id,
    type: 'AGGREGATION',
    config: {
      rowPivots: [
        'timestamp',
      ],
      series: [
        'count()',
      ],
      columnPivots: [],
      sort: [],
      visualization: 'bar',
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

