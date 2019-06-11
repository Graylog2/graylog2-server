// @flow strict
import * as Immutable from 'immutable';
import ViewState from './ViewState';
import { resultHistogram, allMessagesTable } from '../Widget';
import WidgetPosition from '../widgets/WidgetPosition';

const _defaultWidgets = () => {
  const histogram = resultHistogram();
  const messageTable = allMessagesTable();
  const widgets = [
    histogram,
    messageTable,
  ];

  const titles = {
    widget: {
      [histogram.id]: 'Message Count',
      [messageTable.id]: 'All Messages',
    },
  };

  const positions = {
    [histogram.id]: new WidgetPosition(1, 1, 2, Infinity),
    [messageTable.id]: new WidgetPosition(1, 3, 6, Infinity),
  };

  return { titles, widgets, positions };
};

export default () => {
  const { titles, widgets, positions } = _defaultWidgets();
  return ViewState.create()
    .toBuilder()
    .fields(['source', 'message'])
    .titles(titles)
    .widgets(Immutable.List(widgets))
    .widgetPositions(positions)
    .build();
};
