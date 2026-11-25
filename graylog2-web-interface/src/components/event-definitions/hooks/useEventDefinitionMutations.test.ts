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
import * as React from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook } from 'wrappedTestingLibrary/hooks';

import { EventsDefinitions } from '@graylog/server-api';

import { asMock } from 'helpers/mocking';
import type { EventDefinition } from 'components/event-definitions/event-definitions-types';

import useEventDefinitionMutations from './useEventDefinitionMutations';
import { EVENT_DEFINITIONS_QUERY_KEY } from './useEventDefinitions';

jest.mock('@graylog/server-api', () => ({
  EventsDefinitions: { create: jest.fn(), schedule: jest.fn(), remove: jest.fn() },
  EventsEntityTypes: {},
}));

jest.mock('util/UserNotification', () => ({
  __esModule: true,
  default: { error: jest.fn(), success: jest.fn() },
}));

const eventDefinition = { id: 'definition-1', title: 'Failed logins', config: {} } as EventDefinition;

const renderMutations = (queryClient: QueryClient) =>
  renderHook(() => useEventDefinitionMutations(), {
    wrapper: ({ children }: { children: React.ReactNode }) =>
      React.createElement(QueryClientProvider, { client: queryClient }, children as React.ReactElement),
  });

describe('useEventDefinitionMutations', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    jest.clearAllMocks();
    queryClient = new QueryClient({ defaultOptions: { mutations: { retry: false } } });
    asMock(EventsDefinitions.create).mockResolvedValue(
      eventDefinition as unknown as Awaited<ReturnType<typeof EventsDefinitions.create>>,
    );
    asMock(EventsDefinitions.schedule).mockResolvedValue(undefined);
    asMock(EventsDefinitions.remove).mockResolvedValue(undefined);
  });

  it.each([
    ['creating', 'createEventDefinition'],
    ['enabling', 'enableEventDefinition'],
    ['deleting', 'deleteEventDefinition'],
  ] as const)('reloads event definitions after %s one', async (_action, mutation) => {
    const invalidate = jest.spyOn(queryClient, 'invalidateQueries');
    const { result } = renderMutations(queryClient);

    await act(() => result.current[mutation](eventDefinition));

    expect(invalidate).toHaveBeenCalledWith({ queryKey: EVENT_DEFINITIONS_QUERY_KEY });
  });

  it('does not reload event definitions when a change fails', async () => {
    asMock(EventsDefinitions.remove).mockRejectedValue(new Error('Not found'));
    const invalidate = jest.spyOn(queryClient, 'invalidateQueries');
    const { result } = renderMutations(queryClient);

    await act(() => expect(result.current.deleteEventDefinition(eventDefinition)).rejects.toThrow('Not found'));

    expect(invalidate).not.toHaveBeenCalled();
  });
});
