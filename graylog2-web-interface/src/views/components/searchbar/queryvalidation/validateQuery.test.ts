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
