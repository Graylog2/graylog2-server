import Immutable from 'immutable';

import ViewState from 'views/logic/views/ViewState';
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';


// local storage mock for pinned-field-charts
const pinnedFieldCharts = {
  fieldChart1: {
    renderer: 'area',
    interpolation: 'step-after',
    interval: 'minute',
    query: '',
    valuetype: 'count',
    rangetype: 'relative',
    chartid: 'fieldChart1',
    field: 'level',
    createdAt: 1575465944784,
    range: { relative: 300 },
  },
  fieldChart2: {
    renderer: 'bar',
    interpolation: 'linear',
    interval: 'minute',
    query: '',
    valuetype: 'count',
    rangetype: 'relative',
    chartid: 'fieldChart2',
    field: 'level',
    createdAt: 1575466833402,
    range: { relative: 300 },
  },
};

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
  const widgets = Immutable.List([['widget1', widget1], ['widget2', widget2]]);
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


// const viewWidgets = {
//   formatting: undefined,
//   positions: {
//     widget1: {
//       col: 1,
//       height: 2,
//       row: 1,
//       width: 'Infinity',
//     },
//     widget2: {
//       col: 1,
//       height: 6,
//       row: 3,
//       width: 'Infinity',
//     },
//   },
//   selected_fields: undefined,
//   titles: Immutable.Map(),
//   widget_mapping: undefined,
//   widgets: Immutable.List([
//     [
//       'widget1',
//       {
//         config: {
//           column_pivots: [],
//           event_annotation: false,
//           formatting_settings: undefined,
//           rollup: true,
//           row_pivots: [],
//           series: [],
//           sort: [],
//           visualization: undefined,
//           visualization_config: undefined,
//         },
//         filter: undefined,
//         id: 'widget1',
//         query: undefined,
//         streams: [],
//         timerange: undefined,
//         type: 'aggregation',
//       },
//     ],
//     [
//       'widget2',
//       {
//         config: {
//           column_pivots: [],
//           event_annotation: false,
//           formatting_settings: undefined,
//           rollup: true,
//           row_pivots: [],
//           series: [],
//           sort: [],
//           visualization: undefined,
//           visualization_config: undefined,
//         },
//         filter: undefined,
//         id: 'widget2',
//         query: undefined,
//         streams: [],
//         timerange: undefined,
//         type: 'aggregation',
//       },
//     ]]),
// };


// eslint-disable-next-line import/prefer-default-export
export { pinnedFieldCharts, mockFieldCharts, viewState };
