import uuid from 'uuid/v4';

import { WidgetActions } from 'enterprise/stores/WidgetStore';
import pivotForField from 'enterprise/logic/searchtypes/aggregation/PivotGenerator';
import AggregationWidget from 'enterprise/logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from 'enterprise/logic/aggregationbuilder/AggregationWidgetConfig';
import Series from 'enterprise/logic/aggregationbuilder/Series';

export default function (queryId, field) {
  const newWidget = new AggregationWidget(
    uuid(),
    new AggregationWidgetConfig(
      [],
      [pivotForField(field)],
      [Series.forFunction('count()')],
      [],
      'table',
    ),
  );
  WidgetActions.create(newWidget);
}
