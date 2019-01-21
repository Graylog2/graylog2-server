// @flow strict
import uuid from 'uuid/v4';

import { WidgetActions } from 'enterprise/stores/WidgetStore';
import pivotForField from 'enterprise/logic/searchtypes/aggregation/PivotGenerator';
import AggregationWidgetConfig from 'enterprise/logic/aggregationbuilder/AggregationWidgetConfig';
import AggregationWidget from 'enterprise/logic/aggregationbuilder/AggregationWidget';
import Series from 'enterprise/logic/aggregationbuilder/Series';
import type { FieldActionHandler } from './FieldActionHandler';
import FieldType from '../fieldtypes/FieldType';

const ChartActionHandler: FieldActionHandler = (queryId: string, field: string, type: FieldType) => {
  const config = AggregationWidgetConfig.builder()
    .rowPivots([pivotForField('timestamp', type)])
    .series([Series.forFunction(`avg(${field})`)])
    .visualization('line')
    .rollup(true)
    .build();
  const widget = new AggregationWidget(uuid(), config);
  return WidgetActions.create(widget);
};

export default ChartActionHandler;
