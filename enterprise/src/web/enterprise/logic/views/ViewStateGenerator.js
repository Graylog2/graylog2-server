import ViewState from './ViewState';
import { resultHistogram } from '../Widget';

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
    [histogram.id]: {
      col: 1,
      row: 1,
      height: 2,
      width: Infinity,
    },
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