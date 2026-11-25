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
  beforeAll(() => {
    // jsdom has no PointerEvent; without it, pointer events carry no coordinates.
    if (!window.PointerEvent) {
      (window as { PointerEvent: unknown }).PointerEvent = window.MouseEvent;
    }

    HTMLElement.prototype.setPointerCapture = jest.fn();
    HTMLElement.prototype.releasePointerCapture = jest.fn();
  });

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

    expect(await screen.findAllByRole('separator', { name: /resize/i })).toHaveLength(2);
    expect(await screen.findByRole('separator', { name: 'Resize sidebars' })).toHaveAttribute(
      'aria-orientation',
      'vertical',
    );
  });

  it('collapses only the details with their collapse button', async () => {
    await renderWithOpenDetails();

    await userEvent.click(await screen.findByRole('button', { name: 'Collapse sidebar' }));

    await screen.findByRole('button', { name: 'Expand sidebar' });
    await screen.findByText('Assistant chat');

    expect(collapseAssistant).not.toHaveBeenCalled();
  });

  it('collapses all sidebars when their edge is dragged far past the smallest width', async () => {
    await renderWithOpenDetails();
    const handle = await screen.findByRole('separator', { name: 'Resize sidebars' });

    await userEvent.pointer([
      { keys: '[MouseLeft>]', target: handle, coords: { clientX: 1000 } },
      { target: handle, coords: { clientX: 1300 } },
    ]);

    await screen.findByRole('button', { name: 'Expand sidebar' });

    expect(collapseAssistant).toHaveBeenCalledTimes(1);
  });
});
