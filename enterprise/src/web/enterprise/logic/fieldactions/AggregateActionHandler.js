import uuid from 'uuid/v4';

import WidgetActions from 'enterprise/actions/WidgetActions';

export default function (viewId, queryId, field) {
  const newWidget = {
    id: uuid(),
    title: `Values of field ${field}`,
    type: 'AGGREGATION',
    config: {
      rowPivots: [field],
      series: ['count()'],
    },
  };
  WidgetActions.create(viewId, queryId, newWidget);
}
