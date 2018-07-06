import ViewState from './ViewState';
import { resultHistogram } from '../Widget';
import WidgetPosition from '../widgets/WidgetPosition';

const _defaultWidgets = () => {
  const histogram = resultHistogram();
  const widgets = [
    histogram,
  ];

  const titles = {
    widget: {
      [histogram.id]: 'Message Count',
    },
  };

  const positions = {
    [histogram.id]: new WidgetPosition(1, 1, 2, Infinity),
  };

  return { titles, widgets, positions };
};

export default () => {
  const { titles, widgets, positions } = _defaultWidgets();
  return ViewState.create()
    .toBuilder()
    .fields(['source', 'message'])
    .titles(titles)
    .widgets(widgets)
    .widgetPositions(positions)
    .build();
}