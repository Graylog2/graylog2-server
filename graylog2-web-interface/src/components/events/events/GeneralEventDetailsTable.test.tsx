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
import { render, screen, waitFor } from 'wrappedTestingLibrary';

import { asMock } from 'helpers/mocking';
import fetch from 'logic/rest/FetchProvider';
import usePermissions from 'hooks/usePermissions';
import usePluginEntities from 'hooks/usePluginEntities';
import { mockEventData } from 'helpers/mocking/EventAndEventDefinitions_mock';
import RightSidebarProvider from 'contexts/RightSidebarProvider';

import GeneralEventDetailsTable from './GeneralEventDetailsTable';

jest.mock('logic/rest/FetchProvider', () => jest.fn(() => Promise.resolve({ event_definition: {}, context: {} })));
jest.mock('hooks/usePermissions');
jest.mock('hooks/usePluginEntities');

const permitted = (allowed: boolean) =>
  asMock(usePermissions).mockReturnValue({
    isPermitted: () => allowed,
    isAnyPermitted: () => allowed,
    isAdmin: false,
  });

const renderTable = () =>
  render(
    <RightSidebarProvider>
      <GeneralEventDetailsTable
        event={mockEventData.event}
        attributesList={[{ id: 'priority', title: 'Priority' }]}
        meta={{ context: { event_definitions: {} } }}
      />
    </RightSidebarProvider>,
  );

describe('GeneralEventDetailsTable', () => {
  beforeEach(() => {
    asMock(usePluginEntities).mockReturnValue([]);
    asMock(fetch).mockClear();
  });

  it('does not load the event definition for users who may not read it', async () => {
    permitted(false);
    renderTable();

    await screen.findByText('Priority');

    expect(fetch).not.toHaveBeenCalled();
  });

  it('loads the event definition for users who may read it', async () => {
    permitted(true);
    renderTable();

    await waitFor(() =>
      expect(fetch).toHaveBeenCalledWith(
        'GET',
        expect.stringContaining(`/events/definitions/${mockEventData.event.event_definition_id}/with-context`),
      ),
    );
  });
});
