import uuid from 'uuid/v4';

import WidgetActions from 'enterprise/actions/WidgetActions';
import { pivotForField } from '../searchtypes/aggregation/PivotGenerator';

export default function (viewId, queryId, field) {
  const widget = {
    id: uuid(),
    type: 'AGGREGATION',
    config: {
      rowPivots: [pivotForField('timestamp')],
      columnPivots: [],
      series: [`sum(${field})`],
      sort: [],
      visualization: 'line',
    },
  };
  WidgetActions.create(viewId, queryId, widget);
}
