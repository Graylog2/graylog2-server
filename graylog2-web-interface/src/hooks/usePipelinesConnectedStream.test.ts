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
import { renderHook } from 'wrappedTestingLibrary/hooks';
import { waitFor } from 'wrappedTestingLibrary';

import { Streams } from '@graylog/server-api';

import asMock from 'helpers/mocking/AsMock';

import usePipelinesConnectedStream from './usePipelinesConnectedStream';

jest.mock('@graylog/server-api', () => ({
  Streams: {
    getConnectedPipelinesForStreams: jest.fn(),
  },
}));

describe('usePipelinesConnectedStream', () => {
  it('batches subsequent requests', async () => {
    asMock(Streams.getConnectedPipelinesForStreams).mockResolvedValue({
      foo: [],
      bar: [],
      baz: [],
    });

    renderHook(() => {
      usePipelinesConnectedStream('foo');
      usePipelinesConnectedStream('bar');
      usePipelinesConnectedStream('baz');
    });

    await waitFor(() => {
      expect(Streams.getConnectedPipelinesForStreams).toHaveBeenCalledWith({ stream_ids: ['foo', 'bar', 'baz'] });
    });
  });
});
