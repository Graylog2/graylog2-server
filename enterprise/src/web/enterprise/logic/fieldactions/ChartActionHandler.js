// @flow strict
import uuid from 'uuid/v4';
import * as Immutable from 'immutable';

import { WidgetActions } from 'enterprise/stores/WidgetStore';
import pivotForField from 'enterprise/logic/searchtypes/aggregation/PivotGenerator';
import AggregationWidgetConfig from 'enterprise/logic/aggregationbuilder/AggregationWidgetConfig';
import AggregationWidget from 'enterprise/logic/aggregationbuilder/AggregationWidget';
import Series from 'enterprise/logic/aggregationbuilder/Series';
import { FieldTypesStore } from 'enterprise/stores/FieldTypesStore';
import type { FieldActionHandler } from './FieldActionHandler';
import FieldType from '../fieldtypes/FieldType';
import FieldTypeMapping from '../fieldtypes/FieldTypeMapping';

const TIMESTAMP_FIELD = 'timestamp';

const fieldTypeFor = (fieldName: string, queryId: string): FieldType => {
  const _fieldTypes = FieldTypesStore.getInitialState();
  if (!_fieldTypes) {
    return FieldType.Unknown;
  }

  const { queryFields, all } = _fieldTypes;

  const fieldTypes: Immutable.List<FieldTypeMapping> = (!queryFields || queryFields.get(queryId, Immutable.List()).isEmpty()) ? all : queryFields.get(queryId, Immutable.List());

  if (!fieldTypes) {
    return FieldType.Unknown;
  }

  const mapping: FieldTypeMapping = (fieldTypes: Immutable.List<FieldTypeMapping>)
    .find((m: FieldTypeMapping) => m.name === fieldName, null, new FieldTypeMapping(fieldName, FieldType.Unknown));
  return mapping.type;
};

const ChartActionHandler: FieldActionHandler = (queryId: string, field: string) => {
  const config = AggregationWidgetConfig.builder()
    .rowPivots([pivotForField(TIMESTAMP_FIELD, fieldTypeFor(TIMESTAMP_FIELD, queryId))])
    .series([Series.forFunction(`avg(${field})`)])
    .visualization('line')
    .rollup(true)
    .build();
  const widget = new AggregationWidget(uuid(), config);
  return WidgetActions.create(widget);
};

export default ChartActionHandler;
