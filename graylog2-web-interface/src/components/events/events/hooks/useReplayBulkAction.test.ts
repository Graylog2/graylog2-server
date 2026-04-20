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
import { act } from '@testing-library/react';

import { asMock } from 'helpers/mocking';
import useHistory from 'routing/useHistory';
import useLocation from 'routing/useLocation';
import mockHistory from 'helpers/mocking/mockHistory';
import Store from 'logic/local-storage/Store';
import type { Event } from 'components/events/events/types';

import useReplayBulkAction from './useReplayBulkAction';

jest.mock('routing/useHistory');
jest.mock('routing/useLocation', () => jest.fn(() => ({ pathname: '/alerts', search: '', state: {}, key: '', hash: '' })));
jest.mock('logic/local-storage/Store', () => ({ sessionSet: jest.fn() }));
jest.mock('logic/generateId', () => jest.fn(() => 'mock-session-id'));
jest.mock('components/events/events/hooks/useSendEventActionTelemetry', () => jest.fn(() => jest.fn()));

const createEvent = (id: string): Event => ({
  id,
  event_definition_id: 'event-def-1',
  event_definition_type: 'aggregation-v1',
  priority: 1,
  timestamp: '2024-01-01T00:00:00.000Z',
  timerange_start: '2024-01-01T00:00:00.000Z',
  timerange_end: '2024-01-02T00:00:00.000Z',
  key: 'key-1',
  fields: {},
  group_by_fields: {},
  source_streams: ['000000000000000000000001'],
  replay_info: { timerange_start: '2024-01-01', timerange_end: '2024-01-02', query: '', streams: [] },
  alert: undefined,
  message: '',
  timestamp_processing: null,
});

describe('useReplayBulkAction', () => {
  let history: ReturnType<typeof mockHistory>;

  beforeEach(() => {
    history = mockHistory();
    asMock(useHistory).mockReturnValue(history);
    asMock(useLocation).mockReturnValue({ pathname: '/alerts', search: '', state: {}, key: '', hash: '' });
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('navigates to single-event replay search when only one event is selected', () => {
    const events = [createEvent('event-1')];
    const { result } = renderHook(() => useReplayBulkAction(events));

    act(() => result.current());

    expect(history.push).toHaveBeenCalledWith('/alerts/event-1/replay-search');
  });

  it('does not store session data when only one event is selected', () => {
    const events = [createEvent('event-1')];
    const { result } = renderHook(() => useReplayBulkAction(events));

    act(() => result.current());

    expect(Store.sessionSet).not.toHaveBeenCalled();
  });

  it('navigates to bulk replay search when multiple events are selected', () => {
    const events = [createEvent('event-1'), createEvent('event-2')];
    const { result } = renderHook(() => useReplayBulkAction(events));

    act(() => result.current());

    expect(history.push).toHaveBeenCalledWith('/alerts/replay-search?replaySessionId=mock-session-id');
  });

  it('stores session data when multiple events are selected', () => {
    const events = [createEvent('event-1'), createEvent('event-2')];
    const { result } = renderHook(() => useReplayBulkAction(events));

    act(() => result.current());

    expect(Store.sessionSet).toHaveBeenCalledWith('mock-session-id', {
      initialEventIds: ['event-1', 'event-2'],
      returnUrl: '/alerts',
    });
  });
});
