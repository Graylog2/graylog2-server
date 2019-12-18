import Immutable from 'immutable';

import ViewState from 'views/logic/views/ViewState';
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';

// local storage mock for pinned-field-charts
const mockFieldCharts = ({
  renderer = 'line',
  interpolation = 'linear',
  interval = 'minute',
  valuetype = 'count',
  rangetype = 'relative',
  field = 'level',
}) => ({
  'field-chart-id': {
    renderer,
    interpolation,
    interval,
    valuetype,
    rangetype,
    field,
    query: '',
    chartid: 'field-chart-id',
    createdAt: 1575465944784,
    range: { relative: 300 },
  },
});

const viewState = () => {
  const widget1 = AggregationWidget.builder()
    .id('widget1')
    .config(AggregationWidgetConfig.builder().build())
    .build();
  const widget2 = AggregationWidget.builder()
    .id('widget2')
    .config(AggregationWidgetConfig.builder().build())
    .build();
  const widgets = Immutable.List([widget1, widget2]);
  const positions = {
    widget1: new WidgetPosition(1, 1, 2, Infinity),
    widget2: new WidgetPosition(1, 3, 6, Infinity),
  };
  return ViewState.create()
    .toBuilder()
    .widgets(widgets)
    .widgetPositions(positions)
    .build();
};

// eslint-disable-next-line import/prefer-default-export
export { mockFieldCharts, viewState };
