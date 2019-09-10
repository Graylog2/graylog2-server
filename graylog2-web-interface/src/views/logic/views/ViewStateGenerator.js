// @flow strict
import * as Immutable from 'immutable';

import WidgetPosition from '../widgets/WidgetPosition';
import View from './View';
import ViewState from './ViewState';
import { resultHistogram, allMessagesTable } from '../Widget';
import type { ViewType } from './View';

const _defaultWidgets = {
  [View.Type.Search]: () => {
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
  },
  [View.Type.Dashboard]: () => {
    const widgets = [];
    const titles = {};
    const positions = {};

    return { titles, widgets, positions };
  },
};

export default (type: ViewType) => {
  const { titles, widgets, positions } = _defaultWidgets[type]();
  return ViewState.create()
    .toBuilder()
    .titles(titles)
    .widgets(Immutable.List(widgets))
    .widgetPositions(positions)
    .build();
};
