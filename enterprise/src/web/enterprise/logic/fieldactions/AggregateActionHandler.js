import uuid from 'uuid/v4';

import WidgetActions from 'enterprise/actions/WidgetActions';
import { pivotForField } from '../searchtypes/aggregation/PivotGenerator';

export default function (viewId, queryId, field) {
  const newWidget = {
    id: uuid(),
    title: `Values of field ${field}`,
    type: 'AGGREGATION',
    config: {
      rowPivots: [pivotForField(field)],
      series: ['count()'],
    },
  };
  WidgetActions.create(viewId, queryId, newWidget);
}
