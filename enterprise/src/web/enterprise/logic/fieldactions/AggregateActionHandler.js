import uuid from 'uuid/v4';

import WidgetActions from 'enterprise/actions/WidgetActions';
import { pivotForField } from '../searchtypes/aggregation/PivotGenerator';
import AggregationWidget from '../aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from '../aggregationbuilder/AggregationWidgetConfig';

export default function (viewId, queryId, field) {
  const newWidget = new AggregationWidget(
    uuid(),
    'aggregation',
    new AggregationWidgetConfig(
      [],
      [pivotForField(field)],
      ['count()'],
      [],
      'table',
    ),
  );
  WidgetActions.create(viewId, queryId, newWidget);
}
