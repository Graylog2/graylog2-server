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
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, act, waitFor } from 'wrappedTestingLibrary/hooks';

import { StreamRules } from '@graylog/server-api';

import asMock from 'helpers/mocking/AsMock';
import useStreamRuleMutations from 'hooks/useStreamRuleMutations';

type UpdateStreamRuleResponse = Awaited<ReturnType<typeof StreamRules.update>>;

jest.mock('@graylog/server-api', () => ({
  StreamRules: {
    create: jest.fn(),
    update: jest.fn(),
    remove: jest.fn(),
  },
}));

jest.mock('util/UserNotification', () => ({
  error: jest.fn(),
}));

const streamRule = {
  id: 'rule-id',
  stream_id: 'stream-id',
  field: 'source',
  type: 1,
  value: 'example',
  inverted: false,
  description: 'desc',
};

const expectedRequest = {
  field: 'source',
  type: 1,
  value: 'example',
  inverted: false,
  description: 'desc',
};

const updateResponse: UpdateStreamRuleResponse = {
  streamrule_id: 'rule-id',
};

const wrapper = ({ children }) => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });

  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
};

describe('useStreamRuleMutations', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('strips server-managed fields when creating a stream rule', async () => {
    asMock(StreamRules.create).mockResolvedValue(undefined);

    const { result } = renderHook(() => useStreamRuleMutations(), { wrapper });

    await act(async () => {
      await result.current.createStreamRule({ streamId: 'stream-id', data: streamRule });
    });

    await waitFor(() => expect(StreamRules.create).toHaveBeenCalledWith('stream-id', expectedRequest));
  });

  it('strips server-managed fields when updating a stream rule', async () => {
    asMock(StreamRules.update).mockResolvedValue(updateResponse);

    const { result } = renderHook(() => useStreamRuleMutations(), { wrapper });

    await act(async () => {
      await result.current.updateStreamRule({ streamId: 'stream-id', streamRuleId: 'rule-id', data: streamRule });
    });

    await waitFor(() => expect(StreamRules.update).toHaveBeenCalledWith('stream-id', 'rule-id', expectedRequest));
  });
});
