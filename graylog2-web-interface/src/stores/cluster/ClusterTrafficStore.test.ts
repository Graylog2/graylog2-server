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
import { waitFor } from 'wrappedTestingLibrary';

import fetch from 'logic/rest/FetchProvider';
import * as URLUtils from 'util/URLUtils';

import { ClusterTrafficActions } from './ClusterTrafficStore';

jest.mock('logic/rest/FetchProvider', () => jest.fn(() => Promise.resolve()));

describe('ClusterTrafficStore', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('calls the traffic API and passing the days', async () => {
    ClusterTrafficActions.getTraffic(123);

    await waitFor(() => expect(fetch).toHaveBeenCalledTimes(1));

    expect(fetch).toHaveBeenCalledWith('GET', URLUtils.qualifyUrl('/system/cluster/traffic?days=123&daily=false'));
  });
});
