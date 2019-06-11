// @flow strict
import * as Immutable from 'immutable';

import Widget from 'enterprise/logic/widgets/Widget';
import { WidgetActions } from 'enterprise/stores/WidgetStore';
import pivotForField from 'enterprise/logic/searchtypes/aggregation/PivotGenerator';
import AggregationWidgetConfig from 'enterprise/logic/aggregationbuilder/AggregationWidgetConfig';
import AggregationWidget from 'enterprise/logic/aggregationbuilder/AggregationWidget';
import Series from 'enterprise/logic/aggregationbuilder/Series';
import { FieldTypesStore } from 'enterprise/stores/FieldTypesStore';
import type { FieldTypeMappingsList } from 'enterprise/stores/FieldTypesStore';
import type { FieldActionHandler } from './FieldActionHandler';
import FieldType from '../fieldtypes/FieldType';
import FieldTypeMapping from '../fieldtypes/FieldTypeMapping';
import type { ActionContexts } from '../ActionContext';

const TIMESTAMP_FIELD = 'timestamp';

const fieldTypeFor = (fieldName: string, queryId: string): FieldType => {
  const _fieldTypes = FieldTypesStore.getInitialState();
  if (!_fieldTypes) {
    return FieldType.Unknown;
  }

  const { queryFields, all } = _fieldTypes;

  const fieldTypes: FieldTypeMappingsList = (!queryFields || queryFields.get(queryId, Immutable.List()).isEmpty()) ? all : queryFields.get(queryId, Immutable.List());

  if (!fieldTypes) {
    return FieldType.Unknown;
  }

  const mapping: FieldTypeMapping = (fieldTypes: FieldTypeMappingsList)
    .find((m: FieldTypeMapping) => m.name === fieldName, null, new FieldTypeMapping(fieldName, FieldType.Unknown));
  return mapping.type;
};

const ChartActionHandler: FieldActionHandler = (queryId: string, field: string, _: FieldType, context: ActionContexts) => {
  const config = AggregationWidgetConfig.builder()
    .rowPivots([pivotForField(TIMESTAMP_FIELD, fieldTypeFor(TIMESTAMP_FIELD, queryId))])
    .series([Series.forFunction(`avg(${field})`)])
    .visualization('line')
    .rollup(true)
    .build();
  const { widget: origWidget = Widget.empty() } = context;
  const widgetBuilder = AggregationWidget.builder()
    .newId()
    .config(config);

  if (origWidget.filter) {
    widgetBuilder.filter(origWidget.filter);
  }
  const widget = widgetBuilder.build();
  return WidgetActions.create(widget);
};

export default ChartActionHandler;
