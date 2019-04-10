import uuid from 'uuid/v4';

import { PluginStore } from 'graylog-web-plugin/plugin';

import { TIMESTAMP_FIELD } from 'enterprise/Constants';
import pivotForField from './searchtypes/aggregation/PivotGenerator';
import AggregationWidget from './aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from './aggregationbuilder/AggregationWidgetConfig';
import Series from './aggregationbuilder/Series';
import FieldType from './fieldtypes/FieldType';

const widgetsKey = 'enterpriseWidgets';

export function widgetDefinition(type) {
  return PluginStore.exports(widgetsKey)
    .find(widget => widget.type.toLocaleUpperCase() === type.toLocaleUpperCase());
}

export const resultHistogram = (id = uuid()) => AggregationWidget.builder()
  .id(id)
  .config(
    AggregationWidgetConfig.builder()
      .columnPivots([])
      .rowPivots([
        pivotForField(TIMESTAMP_FIELD, new FieldType('date', [], [])),
      ])
      .series([
        Series.forFunction('count()'),
      ])
      .sort([])
      .visualization('bar')
      .rollup(true)
      .build(),
  )
  .build();
