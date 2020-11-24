/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as Immutable from 'immutable';

import CombinedProvider from 'injection/CombinedProvider';

import View from './View';
import ViewState from './ViewState';
import type { ViewType } from './View';

import { resultHistogram, allMessagesTable } from '../Widgets';
import WidgetPosition from '../widgets/WidgetPosition';
import Widget from '../widgets/Widget';

const { DecoratorsActions } = CombinedProvider.get('Decorators');

type Result = {
  titles: { widget: { [key: string]: string } },
  widgets: Array<Widget>,
  positions: { [key: string]: WidgetPosition },
};

type ViewCreator = (streamId: string | undefined | null) => Promise<Result>;
type DefaultWidgets = Record<ViewType, ViewCreator>;

const _defaultWidgets: DefaultWidgets = {
  [View.Type.Search]: async (streamId: string | undefined | null) => {
    const decorators = await DecoratorsActions.list();
    const streamDecorators = decorators ? decorators.filter((decorator) => decorator.stream === streamId) : [];
    const histogram = resultHistogram();
    const messageTable = allMessagesTable(undefined, streamDecorators);
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
  [View.Type.Dashboard]: async (streamId: string | undefined | null) => {
    const widgets = [];
    const titles = {};
    const positions = {};

    return { titles, widgets, positions } as Result;
  },
};

export default async (type: ViewType, streamId?: string) => {
  const { titles, widgets, positions } = await _defaultWidgets[type](streamId);

  return ViewState.create()
    .toBuilder()
    .titles(titles)
    .widgets(Immutable.List(widgets))
    .widgetPositions(positions)
    .build();
};
