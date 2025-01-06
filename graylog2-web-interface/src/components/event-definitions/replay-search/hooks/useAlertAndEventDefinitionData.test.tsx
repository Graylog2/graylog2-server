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

import {
  mockedMappedAggregation,
  mockEventData,
  mockEventDefinitionTwoAggregations,
} from 'helpers/mocking/EventAndEventDefinitions_mock';
import asMock from 'helpers/mocking/AsMock';
import useAlertAndEventDefinitionData from 'components/event-definitions/replay-search/hooks/useAlertAndEventDefinitionData';
import useEventById from 'hooks/useEventById';
import useEventDefinition from 'hooks/useEventDefinition';

jest.mock('hooks/useEventById');
jest.mock('hooks/useEventDefinition');

const mockedHookData = {
  alertId: mockEventData.event.id,
  definitionId: mockEventData.event.event_definition_id,
  definitionTitle: mockEventDefinitionTwoAggregations.title,
  eventData: mockEventData.event,
  eventDefinition: mockEventDefinitionTwoAggregations,
  aggregations: mockedMappedAggregation,
};

const hookResultBase = {
  refetch: () => {},
  isLoading: false,
  isFetched: true,
} as const;

describe('useAlertAndEventDefinitionData', () => {
  beforeEach(() => {
    asMock(useEventDefinition).mockReturnValue({
      ...hookResultBase,
      data: {
        eventDefinition: mockEventDefinitionTwoAggregations,
        aggregations: mockedMappedAggregation,
      },
    });
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should return expected data for alert page', async () => {
    const eventId = 'event-id-1';

    asMock(useEventById).mockReturnValue({
      ...hookResultBase,
      data: { ...mockEventData.event, id: eventId, alert: true },
    });

    const { result } = renderHook(() => useAlertAndEventDefinitionData(eventId));

    await expect(result.current).toEqual(mockedHookData);
  });

  it('should return expected data for event page', async () => {
    const eventId = 'event-id-2';

    asMock(useEventById).mockReturnValue({
      ...hookResultBase,
      data: { ...mockEventData.event, id: eventId, alert: false },
    });

    const { result } = renderHook(() => useAlertAndEventDefinitionData(eventId));

    await expect(result.current).toEqual({
      ...mockedHookData,
      eventData: { ...mockEventData.event, id: eventId, alert: false },
      alertId: eventId,
    });
  });

  it('should return expected data for event definition', async () => {
    asMock(useEventById).mockReturnValue({
      ...hookResultBase,
      data: undefined,
    });

    const { result } = renderHook(() => useAlertAndEventDefinitionData(undefined, mockEventDefinitionTwoAggregations.id));

    await expect(result.current).toEqual({
      ...mockedHookData,
      eventData: undefined,
      alertId: undefined,
      isLoading: false,
    });
  });
});
