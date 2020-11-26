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
import View from './View';
import ViewStateGenerator from './ViewStateGenerator';

import MessagesWidget from '../widgets/MessagesWidget';

const mockList = jest.fn((...args) => Promise.resolve([]));

jest.mock('injection/CombinedProvider', () => ({
  get: (type) => ({
    Decorators: {
      DecoratorsActions: {
        list: (...args) => mockList(...args),
      },
    },
  })[type],
}));

describe('ViewStateGenerator', () => {
  beforeEach(() => {
    jest.resetAllMocks();
  });

  it('adds message table to widgets', async () => {
    const result = await ViewStateGenerator(View.Type.Search);
    const messageTableWidget = result.widgets.find((widget) => widget.type === MessagesWidget.type);

    expect(messageTableWidget).toBeDefined();
  });

  it('adds decorators for current stream to message table', async () => {
    mockList.mockReturnValue(Promise.resolve([
      { id: 'decorator1', stream: 'foobar', order: 0, type: 'something' },
      { id: 'decorator2', stream: 'different', order: 0, type: 'something' },
    ]));

    const result = await ViewStateGenerator(View.Type.Search, 'foobar');

    expect(mockList).toHaveBeenCalledWith();

    const messageTableWidget = result.widgets.find((widget) => widget.type === MessagesWidget.type);

    if (!messageTableWidget) {
      throw new Error('Unable to find message table widget in generated view state.');
    }

    expect(messageTableWidget.config.decorators).toEqual([{ id: 'decorator1', stream: 'foobar', order: 0, type: 'something' }]);
  });

  it('adds decorators for default search to message table if stream id is `null`', async () => {
    mockList.mockReturnValue(Promise.resolve([
      { id: 'decorator1', stream: 'foobar', order: 0, type: 'something' },
      { id: 'decorator2', stream: null, order: 0, type: 'something' },
    ]));

    const result = await ViewStateGenerator(View.Type.Search, null);

    expect(mockList).toHaveBeenCalledWith();

    const messageTableWidget = result.widgets.find((widget) => widget.type === MessagesWidget.type);

    if (!messageTableWidget) {
      throw new Error('Unable to find message table widget in generated view state.');
    }

    expect(messageTableWidget.config.decorators).toEqual([{ id: 'decorator2', stream: null, order: 0, type: 'something' }]);
  });

  it('does not add decorators for current stream to message table if none exist for this stream', async () => {
    mockList.mockReturnValue(Promise.resolve([
      { id: 'decorator1', stream: 'foobar', order: 0, type: 'something' },
      { id: 'decorator2', stream: null, order: 0, type: 'something' },
    ]));

    const result = await ViewStateGenerator(View.Type.Search, 'otherstream');

    expect(mockList).toHaveBeenCalledWith();

    const messageTableWidget = result.widgets.find((widget) => widget.type === MessagesWidget.type);

    if (!messageTableWidget) {
      throw new Error('Unable to find message table widget in generated view state.');
    }

    expect(messageTableWidget.config.decorators).toEqual([]);
  });

  it('does not add decorators for current stream to message table if none exist at all', async () => {
    const result = await ViewStateGenerator(View.Type.Search, 'otherstream');

    expect(mockList).toHaveBeenCalledWith();

    const messageTableWidget = result.widgets.find((widget) => widget.type === MessagesWidget.type);

    if (!messageTableWidget) {
      throw new Error('Unable to find message table widget in generated view state.');
    }

    expect(messageTableWidget.config.decorators).toEqual([]);
  });

  it('does not add decorators for default search to message table if none exist at all', async () => {
    const result = await ViewStateGenerator(View.Type.Search, null);

    expect(mockList).toHaveBeenCalledWith();

    const messageTableWidget = result.widgets.find((widget) => widget.type === MessagesWidget.type);

    if (!messageTableWidget) {
      throw new Error('Unable to find message table widget in generated view state.');
    }

    expect(messageTableWidget.config.decorators).toEqual([]);
  });
});
