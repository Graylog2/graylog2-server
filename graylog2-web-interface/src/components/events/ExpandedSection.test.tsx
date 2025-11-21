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
import { render, screen } from 'wrappedTestingLibrary';

import MetaDataContext from 'components/common/EntityDataTable/contexts/MetaDataContext';
import { eventsTableElements } from 'components/events/Constants';
import { events as eventsFixtures } from 'fixtures/events';
import { useTableFetchContext } from 'components/common/PaginatedEntityTable';
import { asMock } from 'helpers/mocking';
import type { Attribute } from 'stores/PaginationTypes';
import type { Event } from 'components/events/events/types';

import ExpandedSection from './ExpandedSection';

jest.mock('components/common/EntityDataTable/hooks/useUserLayoutPreferences', () =>
  jest.fn(() => ({ data: {}, isInitialLoading: false })),
);
jest.mock('components/common/PaginatedEntityTable', () => ({
  useTableFetchContext: jest.fn(),
}));
jest.mock('components/event-definitions/hooks/useEventDefinitions', () => ({
  useGetEventDefinition: () => ({
    data: { eventDefinition: { event_procedure: '' } },
    isFetching: false,
  }),
}));

describe('ExpandedSection', () => {
  const attributes: Attribute[] = [
    { id: 'id', title: 'ID' },
    { id: 'fields', title: 'Additional Fields' },
  ];

  asMock(useTableFetchContext).mockReturnValue({
    searchParams: {
      page: 1,
      pageSize: 10,
      query: '',
      sort: { attributeId: 'timestamp', direction: 'desc' },
    },
    refetch: jest.fn(),
    attributes,
    entityTableId: eventsTableElements.defaultLayout.entityTableId,
  });

  const baseEvent = { ...eventsFixtures[0], fields: { field: 'The Additional Field' } } as unknown as Event;

  const renderSUT = () =>
    render(
      <MetaDataContext.Provider value={{ meta: {} }}>
        <ExpandedSection defaultLayout={eventsTableElements.defaultLayout} event={baseEvent} />
      </MetaDataContext.Provider>,
    );

  it('renders event attributes provided by table fetch context', async () => {
    renderSUT();

    await screen.findByText(baseEvent.id);
    await screen.findByText('The Additional Field');
  });
});
