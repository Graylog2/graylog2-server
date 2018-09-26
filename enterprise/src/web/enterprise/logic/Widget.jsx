import uuid from 'uuid/v4';

import { PluginStore } from 'graylog-web-plugin/plugin';
import pivotForField from './searchtypes/aggregation/PivotGenerator';
import AggregationWidget from './aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from './aggregationbuilder/AggregationWidgetConfig';
import Series from './aggregationbuilder/Series';

const widgetsKey = 'enterpriseWidgets';

export function widgetDefinition(type) {
  return PluginStore.exports(widgetsKey)
    .find(widget => widget.type.toLocaleUpperCase() === type.toLocaleUpperCase());
}

export const resultHistogram = (id = uuid()) => {
  const config = {
    rowPivots: [
      pivotForField('timestamp'),
    ],
    series: [
      Series.forFunction('count()'),
    ],
    columnPivots: [],
    sort: [],
    visualization: 'bar',
  };
  return new AggregationWidget(id, new AggregationWidgetConfig(config.columnPivots, config.rowPivots, config.series, config.sort, config.visualization));
};
