import uuid from 'uuid/v4';

import { WidgetActions } from 'enterprise/stores/WidgetStore';
import { pivotForField } from 'enterprise/logic/searchtypes/aggregation/PivotGenerator';
import AggregationWidgetConfig from 'enterprise/logic/aggregationbuilder/AggregationWidgetConfig';
import AggregationWidget from 'enterprise/logic/aggregationbuilder/AggregationWidget';

export default function (queryId, field) {
  const config = AggregationWidgetConfig.builder()
    .rowPivots([pivotForField('timestamp')])
    .series([`avg(${field})`])
    .visualization('line')
    .build();
  const widget = new AggregationWidget(uuid(), config);
  WidgetActions.create(widget);
}
