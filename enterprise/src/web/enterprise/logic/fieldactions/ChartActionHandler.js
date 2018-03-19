import uuid from 'uuid/v4';

import WidgetActions from 'enterprise/actions/WidgetActions';

export default function (viewId, queryId, field) {
  const widget = {
    id: uuid(),
    title: field,
    type: 'AGGREGATION',
    config: {
      rowPivots: ['timestamp'],
      series: [`sum(${field})`],
      visualization: 'line',
    },
  };
  WidgetActions.create(viewId, queryId, widget);
}
