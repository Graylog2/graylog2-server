import uuid from 'uuid/v4';

import { WidgetActions } from 'enterprise/stores/WidgetStore';

export default () => {
  // TODO: Replace with proper object.
  WidgetActions.create({
    id: uuid(),
    title: 'Alert Status',
    type: 'ALERT_STATUS',
    config: {
      title: 'Alert Status',
      triggered: false,
      bgColor: '#8bc34a',
      triggeredBgColor: '#d32f2f',
      text: 'OK',
    },
  });
};
