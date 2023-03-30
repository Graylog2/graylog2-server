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
import React from 'react';
import * as Immutable from 'immutable';
import { fireEvent, render, screen, waitFor } from 'wrappedTestingLibrary';

import { adminUser } from 'fixtures/users';
import MockStore from 'helpers/mocking/StoreMock';
import asMock from 'helpers/mocking/AsMock';
import useCurrentUser from 'hooks/useCurrentUser';
import { ClusterTrafficActions, ClusterTrafficStore } from 'stores/cluster/ClusterTrafficStore';

import GraylogClusterOverview from './GraylogClusterOverview';

const trafficResponse = {
  from: '2022-03-31T00:00:00.000Z',
  to: '2022-09-27T09:41:10.926Z',
  input: {
    '2022-09-16T13:00:00.000Z': 0,
    '2022-09-20T18:00:00.000Z': 0,
    '2022-09-20T19:00:00.000Z': 0,
    '2022-09-21T08:00:00.000Z': 20218553,
    '2022-09-21T09:00:00.000Z': 7867447,
    '2022-09-26T10:00:00.000Z': 7942929,
    '2022-09-26T11:00:00.000Z': 27529017,
    '2022-09-26T12:00:00.000Z': 29165527,
    '2022-09-26T13:00:00.000Z': 29188019,
    '2022-09-26T14:00:00.000Z': 29161963,
    '2022-09-26T15:00:00.000Z': 29262878,
    '2022-09-26T16:00:00.000Z': 9254935,
    '2022-09-27T08:00:00.000Z': 2145529,
    '2022-09-27T09:00:00.000Z': 1053461,
  },
  output: {
    '2022-09-16T13:00:00.000Z': 0,
    '2022-09-20T18:00:00.000Z': 0,
    '2022-09-20T19:00:00.000Z': 0,
    '2022-09-21T08:00:00.000Z': 45410034,
    '2022-09-21T09:00:00.000Z': 17605708,
    '2022-09-26T10:00:00.000Z': 17903942,
    '2022-09-26T11:00:00.000Z': 61783654,
    '2022-09-26T12:00:00.000Z': 65182052,
    '2022-09-26T13:00:00.000Z': 65215928,
    '2022-09-26T14:00:00.000Z': 65191578,
    '2022-09-26T15:00:00.000Z': 65400227,
    '2022-09-26T16:00:00.000Z': 20685337,
    '2022-09-27T08:00:00.000Z': 4837416,
    '2022-09-27T09:00:00.000Z': 2366005,
  },
  decoded: {
    '2022-09-16T13:00:00.000Z': 0,
    '2022-09-20T18:00:00.000Z': 0,
    '2022-09-20T19:00:00.000Z': 0,
    '2022-09-21T08:00:00.000Z': 40711455,
    '2022-09-21T09:00:00.000Z': 15760433,
    '2022-09-26T10:00:00.000Z': 16046259,
    '2022-09-26T11:00:00.000Z': 55382557,
    '2022-09-26T12:00:00.000Z': 58412313,
    '2022-09-26T13:00:00.000Z': 58457791,
    '2022-09-26T14:00:00.000Z': 58404293,
    '2022-09-26T15:00:00.000Z': 58607214,
    '2022-09-26T16:00:00.000Z': 18537004,
    '2022-09-27T08:00:00.000Z': 4351934,
    '2022-09-27T09:00:00.000Z': 2138078,
  },
};

jest.mock('hooks/useCurrentUser');

jest.mock('stores/cluster/ClusterTrafficStore', () => ({
  ClusterTrafficStore: MockStore(
    ['getInitialState', jest.fn(() => ({
      traffic: undefined,
    }))],
  ),
  ClusterTrafficActions: {
    getTraffic: jest.fn((_days: number) => ({ traffic: undefined })),
  },
}));

describe('GraylogClusterOverview', () => {
  beforeEach(() => {
    const currentUserWithPermissions = adminUser.toBuilder()
      .permissions(Immutable.List(['licenses:read']))
      .build();

    asMock(useCurrentUser).mockReturnValue(currentUserWithPermissions);

    asMock(ClusterTrafficStore.getInitialState).mockReturnValue({
      traffic: trafficResponse,
    });
  });

  afterEach(() => {
    jest.clearAllMocks();
    jest.useRealTimers();
  });

  it('renders GraylogClusterOverview', async () => {
    render(<GraylogClusterOverview />);

    expect(screen.getByText(/Outgoing traffic/)).toBeInTheDocument();

    await waitFor(() => expect(ClusterTrafficActions.getTraffic).toHaveBeenCalledWith(30));

    expect(screen.getByText(/Last 30 days/)).toBeInTheDocument();
  });

  it('renders GraylogClusterOverview and change the days for the traffic graph', async () => {
    const { getByLabelText } = render(<GraylogClusterOverview />);
    const graphDaysSelect = getByLabelText('Days');

    fireEvent.change(graphDaysSelect, { target: { value: 365 } });

    await waitFor(() => expect(ClusterTrafficActions.getTraffic).toHaveBeenCalledWith(365));

    expect(screen.getByText(/Last 365 days/)).toBeInTheDocument();
  });
});
