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
import userEvent from '@testing-library/user-event';

import BulkActions from 'components/events/events/BulkActions';
import { asMock } from 'helpers/mocking';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';
import usePluginEntities from 'hooks/usePluginEntities';
import { events } from 'fixtures/events';
import type { EventAction } from 'views/types';
import type { Event } from 'components/events/events/types';

jest.mock('hooks/usePluginEntities');
jest.mock('components/common/EntityDataTable/hooks/useSelectedEntities');

const mockedSelectedEntitiesData = {
  '01HV0YS4GHDMT30E3EMWQVQNK9': events[0],
  '01HV0YS4GH0VC7DV6A2VGN1VJ0': events[1],
} as { [id: string]: Event};

const mockedEventActions: Array<EventAction> = [
  {
    useCondition: () => true,
    isBulk: true,
    key: 'key-1',
    component: () => <span>I am component with a bulk</span>,
    modal: React.forwardRef(() => <b>I am a modal with a bulk</b>),
  },
  {
    useCondition: () => true,
    isBulk: false,
    key: 'key-2',
    component: () => <span>I am component without a bulk</span>,
    modal: React.forwardRef(() => <b>I am a modal without a bulk</b>),
  },
];
const renderBulkAction = () => render(<BulkActions selectedEntitiesData={mockedSelectedEntitiesData} />);

const openActionsDropdown = async () => userEvent.click(await screen.findByRole('button', {
  name: /bulk actions/i,
}));

describe('Events Bulk Action', () => {
  beforeEach(() => {
    asMock(useSelectedEntities).mockReturnValue({
      selectedEntities: ['01HV0YS4GHDMT30E3EMWQVQNK9', '01HV0YS4GHDMT30E3EMWQVQNK9'],
      setSelectedEntities: () => {},
      selectEntity: () => {},
      deselectEntity: () => {},
      toggleEntitySelect: () => {},
    });
  });

  it('calls usePluginEntities with correct parameter', () => {
    asMock(usePluginEntities).mockReturnValue(mockedEventActions);

    renderBulkAction();

    expect(usePluginEntities).toHaveBeenCalledWith('views.components.eventActions');
  });

  it('render actions only with bulk flag', async () => {
    asMock(usePluginEntities).mockReturnValue(mockedEventActions);

    renderBulkAction();

    await openActionsDropdown();

    await screen.findByText('I am component with a bulk');
    await screen.findByText('I am a modal with a bulk');

    const notBulkComponent = screen.queryByText('I am component without a bulk');
    const notBulkModal = screen.queryByText('I am a modal without a bulk');

    expect(notBulkComponent).not.toBeInTheDocument();
    expect(notBulkModal).not.toBeInTheDocument();
  });
});
