import uuid from 'uuid/v4';

import { PluginStore } from 'graylog-web-plugin/plugin';

import { TIMESTAMP_FIELD, DEFAULT_MESSAGE_FIELDS } from 'views/Constants';
import pivotForField from './searchtypes/aggregation/PivotGenerator';
import AggregationWidget from './aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from './aggregationbuilder/AggregationWidgetConfig';
import MessageWidget from './widgets/MessagesWidget';
import MessageWidgetConfig from './widgets/MessagesWidgetConfig.js';
import Series from './aggregationbuilder/Series';
import FieldType from './fieldtypes/FieldType';

const widgetsKey = 'enterpriseWidgets';

const _findWidgetDefinition = type => PluginStore.exports(widgetsKey)
  .find(widget => widget.type.toLocaleUpperCase() === type.toLocaleUpperCase());

export function widgetDefinition(type) {
  const typeDefinition = _findWidgetDefinition(type);
  if (typeDefinition) {
    return typeDefinition;
  }
  return _findWidgetDefinition('default');
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

export const allMessagesTable = (id = uuid()) => MessageWidget.builder()
  .id(id)
  .config(MessageWidgetConfig.builder()
    .fields(DEFAULT_MESSAGE_FIELDS)
    .showMessageRow(true)
    .build())
  .build();
