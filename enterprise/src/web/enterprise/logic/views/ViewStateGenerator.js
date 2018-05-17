import ViewState from './ViewState';
import { messageList, resultHistogram } from '../Widget';

const _defaultWidgets = () => {
  const histogram = resultHistogram();
  const messages = messageList();
  const widgets = [
    histogram,
    messages,
  ];

  const titles = {
    widget: {
      [histogram.id]: 'Message Count',
      [messages.id]: 'Messages',
    },
  };

  const positions = {
    [histogram.id]: {
      col: 1,
      row: 1,
      height: 2,
      width: 4,
    },
    [messages.id]: {
      col: 1,
      row: 2,
      height: 6,
      width: 6,
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