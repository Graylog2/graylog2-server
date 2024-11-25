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

import { DecoratorsActions } from 'stores/decorators/DecoratorsStore';

import View from './View';
import type { ViewType } from './View';
import ViewState from './ViewState';

import { resultHistogram, allMessagesTable } from '../Widgets';
import WidgetPosition from '../widgets/WidgetPosition';
import type Widget from '../widgets/Widget';

type Result = {
  titles: { widget: { [key: string]: string } },
  widgets: Array<Widget>,
  positions: { [key: string]: WidgetPosition },
};

type ViewCreator = (streamId: string | string[] | undefined | null, streamCategory: string | string[] | undefined | null) => Promise<Result>;
type DefaultWidgets = Record<ViewType, ViewCreator>;

type Decorator = { stream: string | null, category: string | null };

export const matchesDecoratorStream = (streamId: string | string[] | undefined | null) => {
  if (!streamId) {
    return ({ stream }: Decorator) => stream === null;
  }

  if (streamId instanceof Array) {
    return ({ stream }: Decorator) => streamId.includes(stream);
  }

  return ({ stream }: Decorator) => stream === streamId;
};

export const matchesDecoratorStreamCategories = (streamCategory: string | string[] | undefined | null) => {
  if (!streamCategory) {
    return ({ category }: Decorator) => category === null;
  }

  if (streamCategory instanceof Array) {
    return ({ category }: Decorator) => streamCategory.includes(category);
  }

  return ({ category }: Decorator) => category === streamCategory;
};

const _defaultWidgets: DefaultWidgets = {
  [View.Type.Search]: async (streamId: string | string[] | undefined | null, streamCategory: string | string[] | undefined | null) => {
    const decorators = await DecoratorsActions.list();
    const byStreamId = matchesDecoratorStream(streamId);
    const byStreamCategory = matchesDecoratorStreamCategories(streamCategory);
    const streamDecorators = decorators ? decorators.filter(byStreamId) : [];
    const streamCategoryDecorators = decorators?.length ? decorators.filter(byStreamCategory) : [];
    // eslint-disable-next-line no-nested-ternary
    const allDecorators = streamDecorators.length && streamCategoryDecorators.length
      ? [...streamDecorators, ...streamCategoryDecorators]
      : streamDecorators.length
        ? streamDecorators
        : streamCategoryDecorators;
    const histogram = resultHistogram();
    const messageTable = allMessagesTable(undefined, allDecorators);
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

  [View.Type.Dashboard]: async (_streamId: string | string[] | undefined | null) => {
    const widgets = [];
    const titles = {};
    const positions = {};

    return { titles, widgets, positions } as Result;
  },
};

export default async (type: ViewType, streamId?: string | string[] | undefined, streamCategory?: string | string[] | undefined) => {
  const { titles, widgets, positions } = await _defaultWidgets[type](streamId, streamCategory);

  return ViewState.create()
    .toBuilder()
    .titles(titles)
    .widgets(Immutable.List(widgets))
    .widgetPositions(positions)
    .build();
};
