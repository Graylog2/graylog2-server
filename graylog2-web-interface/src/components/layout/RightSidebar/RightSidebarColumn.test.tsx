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

import { asMock } from 'helpers/mocking';
import usePluginEntities from 'hooks/usePluginEntities';
import useRightSidebar from 'hooks/useRightSidebar';
import RightSidebarProvider from 'contexts/RightSidebarProvider';

import RightSidebarColumn from './RightSidebarColumn';

jest.mock('hooks/usePluginEntities');

const collapseAssistant = jest.fn();

const Assistant = () => <p>Assistant chat</p>;
const EventDetails = () => <p>Event details</p>;

const OpenDetails = () => {
  const { openSidebar } = useRightSidebar();

  return (
    <button type="button" onClick={() => openSidebar({ id: 'event', title: 'Event', component: EventDetails })}>
      Open details
    </button>
  );
};

const renderWithOpenDetails = async () => {
  render(
    <RightSidebarProvider>
      <OpenDetails />
      <RightSidebarColumn />
    </RightSidebarProvider>,
  );

  await userEvent.click(await screen.findByRole('button', { name: 'Open details' }));
};

describe('<RightSidebarColumn />', () => {
  beforeEach(() => {
    collapseAssistant.mockClear();
    asMock(usePluginEntities).mockImplementation((entityKey) =>
      entityKey === 'globalSidebars'
        ? [
            {
              key: 'assistant',
              component: Assistant,
              useSidebarState: () => ({ isOpen: true, isCollapsed: false, collapse: collapseAssistant }),
            },
          ]
        : [],
    );
  });

  it('stacks the details above an open global sidebar, with one handle for the width', async () => {
    await renderWithOpenDetails();

    await screen.findByText('Event details');
    await screen.findByText('Assistant chat');
    await screen.findByRole('separator', { name: 'Resize sidebars' });
  });

  it('collapses only the details with their collapse button', async () => {
    await renderWithOpenDetails();

    await userEvent.click(await screen.findByRole('button', { name: 'Collapse sidebar' }));

    await screen.findByRole('button', { name: 'Expand sidebar' });
    await screen.findByText('Assistant chat');

    expect(collapseAssistant).not.toHaveBeenCalled();
  });
});
