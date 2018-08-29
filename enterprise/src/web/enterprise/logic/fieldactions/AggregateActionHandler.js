import uuid from 'uuid/v4';

import { WidgetActions } from 'enterprise/stores/WidgetStore';
import pivotForField from '../searchtypes/aggregation/PivotGenerator';
import AggregationWidget from '../aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from '../aggregationbuilder/AggregationWidgetConfig';

export default function (queryId, field) {
  const newWidget = new AggregationWidget(
    uuid(),
    new AggregationWidgetConfig(
      [],
      [pivotForField(field)],
      ['count()'],
      [],
      'table',
    ),
  );
  WidgetActions.create(newWidget);
}
