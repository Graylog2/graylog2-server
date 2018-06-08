import uuid from 'uuid/v4';

import { WidgetActions } from 'enterprise/stores/WidgetStore';
import { pivotForField } from '../searchtypes/aggregation/PivotGenerator';

export default function (queryId, field) {
  const widget = {
    id: uuid(),
    type: 'AGGREGATION',
    config: {
      rowPivots: [pivotForField('timestamp')],
      columnPivots: [],
      series: [`avg(${field})`],
      sort: [],
      visualization: 'line',
    },
  };
  WidgetActions.create(widget);
}
