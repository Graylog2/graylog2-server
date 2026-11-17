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

import { asMock } from 'helpers/mocking';
import { createSearch } from 'fixtures/searches';
import ViewState from 'views/logic/views/ViewState';
import TextWidget from 'views/logic/widgets/TextWidget';
import TextWidgetConfig from 'views/logic/widgets/TextWidgetConfig';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import { updateViewState } from 'views/logic/slices/viewSlice';
import type { RootState } from 'views/types';

import { addWidget } from './widgetActions';

jest.mock('views/logic/slices/viewSlice', () => ({
  ...jest.requireActual('views/logic/slices/viewSlice'),
  updateViewState: jest.fn(() => async () => {}),
}));

describe('widgetActions', () => {
  const state = {
    view: {
      activeQuery: 'query-id',
      view: createSearch()
        .toBuilder()
        .state(Immutable.Map({ 'query-id': ViewState.create() }))
        .build(),
    },
  } as unknown as RootState;
  const dispatch = jest.fn((action: unknown) => (typeof action === 'function' ? action(dispatch, () => state) : action));
  const widget = TextWidget.builder().id('w1').config(new TextWidgetConfig('Hi')).build();
  const position = new WidgetPosition(1, 1, 2, 6);

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('adds a widget with its title in one change', async () => {
    await addWidget(widget, position, 'Notes')(dispatch, () => state);

    expect(updateViewState).toHaveBeenCalledTimes(1);
    const [queryId, viewState] = asMock(updateViewState).mock.calls[0];

    expect(queryId).toBe('query-id');
    expect(viewState.widgets.map(({ id }) => id).toArray()).toEqual(['w1']);
    expect(viewState.titles.getIn(['widget', 'w1'])).toBe('Notes');
  });

  it('adds a widget without a title', async () => {
    await addWidget(widget, position)(dispatch, () => state);

    const [, viewState] = asMock(updateViewState).mock.calls[0];

    expect(viewState.titles.getIn(['widget', 'w1'])).toBeUndefined();
  });
});
