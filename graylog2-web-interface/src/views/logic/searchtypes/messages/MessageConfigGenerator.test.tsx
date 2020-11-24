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
import MessagesWidget from 'views/logic/widgets/MessagesWidget';
import MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';
import Direction from 'views/logic/aggregationbuilder/Direction';

import MessageConfigGenerator from './MessageConfigGenerator';
import MessageSortConfig from './MessageSortConfig';

describe('MessageConfigGenerator', () => {
  const defaultSort = [new MessageSortConfig('timestamp', Direction.Descending)];

  it('generates basic search type from message widget with a default sort', () => {
    // $FlowFixMe: We need to force this being a `MessagesWidget`
    const widget: MessagesWidget = MessagesWidget.builder()
      .config(
        MessagesWidgetConfig.builder()
          .decorators([])
          .build(),
      ).build();

    const result = MessageConfigGenerator(widget);

    expect(result).toEqual([{ decorators: [], sort: defaultSort, type: 'messages' }]);
  });

  it('adds decorators to search type', () => {
    const decorators = [
      { id: 'decorator1', type: 'something', config: {}, stream: null, order: 0 },
      { id: 'decorator2', type: 'something else', config: {}, stream: null, order: 1 },
    ];
    // $FlowFixMe: We need to force this being a `MessagesWidget`
    const widget: MessagesWidget = MessagesWidget.builder()
      .config(
        MessagesWidgetConfig.builder()
          .decorators(decorators)
          .build(),
      ).build();

    const result = MessageConfigGenerator(widget);

    expect(result).toEqual([{
      decorators: [
        { id: 'decorator1', type: 'something', config: {}, stream: null, order: 0 },
        { id: 'decorator2', type: 'something else', config: {}, stream: null, order: 1 },
      ],
      sort: defaultSort,
      type: 'messages',
    }]);
  });
});
