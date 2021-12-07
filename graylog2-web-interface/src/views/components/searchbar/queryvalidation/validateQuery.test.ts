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
import { waitFor } from 'wrappedTestingLibrary';

import fetch from 'logic/rest/FetchProvider';

import validateQuery from './validateQuery';

jest.mock('logic/rest/FetchProvider', () => jest.fn(() => Promise.resolve()));

describe('validateQuery', () => {
  it('calls validate API', async () => {
    await validateQuery({
      queryString: 'source:',
      timeRange: { type: 'relative', from: 300 },
      streams: ['stream-id'],
      parameters: Immutable.Set(),
      parameterBindings: Immutable.Map(),
    });

    await waitFor(() => expect(fetch).toHaveBeenCalledTimes(1));

    const expectedPayload = {
      query: 'source:',
      filter: undefined,
      timerange: { type: 'relative', from: 300 },
      streams: ['stream-id'],
      parameters: Immutable.Set(),
      parameter_bindings: Immutable.Map(),
    };

    expect(fetch).toHaveBeenCalledWith('POST', expect.any(String), expectedPayload);
  });
});
