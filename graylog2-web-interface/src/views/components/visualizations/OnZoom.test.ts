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
import View from 'views/logic/views/View';
import mockDispatch from 'views/test/mockDispatch';
import type { RootState } from 'views/types';
import { updateGlobalOverride } from 'views/logic/slices/searchExecutionSlice';
import GlobalOverride from 'views/logic/search/GlobalOverride';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import { createSearch } from 'fixtures/searches';
import { setTimerange } from 'views/logic/slices/viewSlice';

import OnZoom from './OnZoom';

jest.mock('views/logic/slices/viewSlice', () => ({
  setTimerange: jest.fn(),
}));

describe('OnZoom', () => {
  const defaultView = createSearch({ queryId: 'query1' });

  it('sets the global override timerange if called from a dashboard', async () => {
    const view = defaultView.toBuilder().type(View.Type.Dashboard).build();

    const dispatch = mockDispatch({ view: { view }, searchExecution: { executionState: SearchExecutionState.empty() } } as RootState);
    dispatch(OnZoom('2020-01-10 13:23:42.000', '2020-01-10 14:23:42.000', 'Europe/Berlin'));

    expect(dispatch).toHaveBeenCalledWith(updateGlobalOverride(GlobalOverride.create({
      from: '2020-01-10T12:23:42.000+00:00',
      to: '2020-01-10T13:23:42.000+00:00',
      type: 'absolute',
    })));
  });

  it('sets the query timerange if called from a dashboard', async () => {
    const view = defaultView.toBuilder().type(View.Type.Search).build();

    const dispatch = mockDispatch({ view: { view, activeQuery: 'query1' }, searchExecution: { executionState: SearchExecutionState.empty() } } as RootState);
    dispatch(OnZoom('2020-01-10 13:23:42.000', '2020-01-10 14:23:42.000', 'Europe/Berlin'));

    expect(setTimerange).toHaveBeenCalledWith('query1', {
      from: '2020-01-10T12:23:42.000+00:00',
      to: '2020-01-10T13:23:42.000+00:00',
      type: 'absolute',
    });
  });
});
