// @flow strict
import * as Immutable from 'immutable';

import CombinedProvider from 'injection/CombinedProvider';
import WidgetPosition from '../widgets/WidgetPosition';
import View from './View';
import ViewState from './ViewState';
import { resultHistogram, allMessagesTable } from '../Widgets';
import type { ViewType } from './View';

const { DecoratorsActions } = CombinedProvider.get('Decorators');

const _defaultWidgets = {
  [View.Type.Search]: async (streamId: ?string) => {
    console.log('Generating new search for stream: ', streamId);
    const decorators = await DecoratorsActions.list() || [];
    console.log('Got decorators: ', decorators);
    const streamDecorators = decorators.filter(decorator => decorator.stream === streamId);
    console.log('Got stream decorators: ', streamDecorators);
    const histogram = resultHistogram();
    const messageTable = allMessagesTable(undefined, streamDecorators);
    console.log('Got message widget: ', messageTable);
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
  // eslint-disable-next-line no-unused-vars
  [View.Type.Dashboard]: async (streamId: ?string) => {
    const widgets = [];
    const titles = {};
    const positions = {};

    return { titles, widgets, positions };
  },
};

export default async (type: ViewType, streamId: ?string) => {
  const { titles, widgets, positions } = await _defaultWidgets[type](streamId);
  return ViewState.create()
    .toBuilder()
    .titles(titles)
    .widgets(Immutable.List(widgets))
    .widgetPositions(positions)
    .build();
};
