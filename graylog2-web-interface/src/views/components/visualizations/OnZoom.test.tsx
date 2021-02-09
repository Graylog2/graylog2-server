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
import Query from 'views/logic/queries/Query';
import { QueriesActions } from 'views/stores/QueriesStore';
import { GlobalOverrideActions } from 'views/stores/GlobalOverrideStore';

import OnZoom from './OnZoom';

const mockGetTimezone = jest.fn(() => ({ timezone: 'UTC' }));

jest.mock('injection/CombinedProvider', () => ({
  get: () => ({ CurrentUserStore: { get: () => mockGetTimezone() } }),
}));

jest.mock('views/stores/QueriesStore', () => ({
  QueriesActions: {
    query: jest.fn(() => Promise.resolve()),
    timerange: jest.fn(() => Promise.resolve()),
  },
}));

jest.mock('views/stores/GlobalOverrideStore', () => ({
  GlobalOverrideActions: {
    timerange: jest.fn(() => Promise.resolve()),
  },
}));

describe('OnZoom', () => {
  it('sets the global override timerange if called from a dashboard', async () => {
    const query = Query.builder().build();

    await OnZoom(query, '2020-01-10 13:23:42.000', '2020-01-10 14:23:42.000', View.Type.Dashboard);

    expect(GlobalOverrideActions.timerange).toHaveBeenCalledWith({
      from: '2020-01-10T13:23:42.000Z',
      to: '2020-01-10T14:23:42.000Z',
      type: 'absolute',
    });
  });

  it('sets the query timerange if called from a dashboard', async () => {
    const query = Query.builder().id('query1').build();

    await OnZoom(query, '2020-01-10 13:23:42.000', '2020-01-10 14:23:42.000', View.Type.Search);

    expect(QueriesActions.timerange).toHaveBeenCalledWith('query1', {
      from: '2020-01-10T13:23:42.000Z',
      to: '2020-01-10T14:23:42.000Z',
      type: 'absolute',
    });
  });

  it('converts the passed time stamps from the user\'s time range to UTC', async () => {
    mockGetTimezone.mockReturnValue({ timezone: 'CET' });
    const query = Query.builder().id('query1').build();

    await OnZoom(query, '2020-01-10 13:23:42.000', '2020-01-10 14:23:42.000', View.Type.Search);

    expect(QueriesActions.timerange).toHaveBeenCalledWith('query1', {
      from: '2020-01-10T12:23:42.000Z',
      to: '2020-01-10T13:23:42.000Z',
      type: 'absolute',
    });
  });
});
