import uuid from 'uuid/v4';

import { PluginStore } from 'graylog-web-plugin/plugin';
import { pivotForField } from './searchtypes/aggregation/PivotGenerator';
import AggregationWidget from './aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from './aggregationbuilder/AggregationWidgetConfig';
import MessagesWidget from './widgets/MessagesWidget';
import MessagesWidgetConfig from './widgets/MessagesWidgetConfig';

const widgetsKey = 'enterpriseWidgets';

export function widgetDefinition(type) {
  return PluginStore.exports(widgetsKey)
    .find(widget => widget.type.toLocaleUpperCase() === type.toLocaleUpperCase());
}

export const messageList = (id = uuid(), fields = []) => {
  return new MessagesWidget(id, new MessagesWidgetConfig(fields, true));
};

export const resultHistogram = (id = uuid()) => {
  const config = {
    rowPivots: [
      pivotForField('timestamp'),
    ],
    series: [
      'count()',
    ],
    columnPivots: [],
    sort: [],
    visualization: 'bar',
  };
  return new AggregationWidget(id, new AggregationWidgetConfig(config.columnPivots, config.rowPivots, config.series, config.sort, config.visualization));
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

