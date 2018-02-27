import uuid from 'uuid/v4';

import WidgetActions from 'enterprise/actions/WidgetActions';

export default function (viewId, queryId, field) {
  console.log(`New action for field ${field}.`);
  const widget = {
    id: uuid(),
    title: field,
    type: 'FIELD_HISTOGRAM',
    config: {
      field: field,
    },
  };
  WidgetActions.create(viewId, queryId, widget);
}
