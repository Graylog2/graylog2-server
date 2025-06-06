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

import useViewsPlugin from 'views/test/testViewsPlugin';
import asMock from 'helpers/mocking/AsMock';
import useAlertAndEventDefinitionData from 'components/event-definitions/replay-search/hooks/useAlertAndEventDefinitionData';
import { SYSTEM_EVENT_DEFINITION_TYPE } from 'components/event-definitions/constants';

import { eventDefinition } from './fixtures';

import ReplaySearch from '../ReplaySearch';

jest.mock('views/pages/SearchPage', () => () => <span>Embedded Search</span>);
jest.mock('components/event-definitions/replay-search/hooks/useAlertAndEventDefinitionData');
jest.mock('views/hooks/useCreateSearch', () => async (search) => search);

describe('ReplaySearch', () => {
  useViewsPlugin();

  it('can replay event definitions only', async () => {
    asMock(useAlertAndEventDefinitionData).mockReturnValue({
      alertId: undefined,
      definitionId: 'eventDefinitionId',
      definitionTitle: eventDefinition.title,
      eventDefinition,
      aggregations: [],
      eventData: undefined,
      isLoading: false,
    });
    render(<ReplaySearch alertId={undefined} definitionId="eventDefinitionId" />);
    await screen.findByText('Embedded Search');
  });

  it('refuses to replay system events', async () => {
    asMock(useAlertAndEventDefinitionData).mockReturnValue({
      alertId: undefined,
      definitionId: 'eventDefinitionId',
      definitionTitle: eventDefinition.title,
      eventDefinition: {
        ...eventDefinition,
        config: {
          ...eventDefinition.config,
          type: SYSTEM_EVENT_DEFINITION_TYPE,
        },
      },
      aggregations: [],
      eventData: undefined,
      isLoading: false,
    });
    render(<ReplaySearch alertId="eventId" definitionId="eventDefinitionId" />);
    await screen.findByText(/Event is a system event/);

    expect(screen.queryByText('Embedded Search')).not.toBeInTheDocument();
  });
});
