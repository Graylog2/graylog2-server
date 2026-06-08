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
import { render, screen, waitFor, within } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import { PluginManifest } from 'graylog-web-plugin/plugin';

import { usePlugin } from 'views/test/testPlugins';
import MenuItem from 'components/bootstrap/menuitem/MenuItem';
import SidebarBulkEventReplay from 'components/events/bulk-replay/SidebarBulkEventReplay';
import EventReplaySelectedProvider from 'contexts/EventReplaySelectedProvider';
import asMock from 'helpers/mocking/AsMock';
import useSessionInitialEventIds from 'components/events/bulk-replay/hooks/useSessionInitialEventIds';

import events from './events.fixtures';

const initialEventIds = ['01JH007XPDF710TPJZT8K2CN3W', '01JH006340WP7HQ5E7P71Q9HHX', '01JH0029TS9PX5ED87TZ1RVRT2'];

jest.mock('components/events/bulk-replay/hooks/useEventsById');
jest.mock('components/events/bulk-replay/hooks/useSessionInitialEventIds');

const markEventAsReviewedFromList = async (eventId: string) => {
  const list = await screen.findByRole('list');

  const markAsReviewedButton = await within(list).findByRole('button', {
    name: new RegExp(`mark event "${eventId}" as reviewed`, 'i'),
  });

  return userEvent.click(markAsReviewedButton);
};

const removeEvent = async (eventId: string) => {
  const removeEventButton = await screen.findByRole('button', {
    name: new RegExp(`remove event "${eventId}" from list`, 'i'),
  });

  return userEvent.click(removeEventButton);
};

const eventByIndex = (index: number) => events[initialEventIds[index]].event;
const eventMessage = (index: number) => eventByIndex(index).message;

const SUT = () => (
  <EventReplaySelectedProvider initialEventIds={initialEventIds} eventsData={events}>
    <SidebarBulkEventReplay />
  </EventReplaySelectedProvider>
);

const bulkAction = jest.fn();
const testPlugin = new PluginManifest(
  {},
  {
    'views.components.eventActions': [
      {
        key: 'test-bulk-action',
        component: ({ events: _events }) => <MenuItem onClick={() => bulkAction(_events)}>Test Action</MenuItem>,
        useCondition: () => true,
        isBulk: true,
      },
    ],
  },
);

describe('SidebarBulkEventReplay', () => {
  usePlugin(testPlugin);

  beforeEach(() => {
    asMock(useSessionInitialEventIds).mockReturnValue(initialEventIds);
  });

  it('renders list of selected events', async () => {
    render(<SUT />);
    const list = await screen.findByRole('list');
    await within(list).findByText(eventMessage(0));
    await within(list).findByText(eventMessage(1));
    await within(list).findByText(eventMessage(2));
  });

  it('clicking delete button removes event from list', async () => {
    render(<SUT />);
    const list = await screen.findByRole('list');
    await within(list).findByText(eventMessage(0));
    await removeEvent(initialEventIds[0]);

    expect(within(list).queryByText(eventMessage(0))).not.toBeInTheDocument();
    await within(list).findByText(eventMessage(1));
    await within(list).findByText(eventMessage(2));
  });

  it('marking events as reviewed shows completion message when all done', async () => {
    render(<SUT />);
    await markEventAsReviewedFromList(initialEventIds[0]);
    await markEventAsReviewedFromList(initialEventIds[1]);
    await markEventAsReviewedFromList(initialEventIds[2]);

    await screen.findByText(
      'You have reviewed all events. You can now select a bulk action to apply to all events listed below.',
    );
  });

  it('allows clicking on specific events', async () => {
    render(<SUT />);
    const list = await screen.findByRole('list');
    const secondEvent = await within(list).findByText(eventMessage(1));
    await userEvent.click(secondEvent);

    const firstEvent = await within(list).findByText(eventMessage(0));
    await userEvent.click(firstEvent);
  });

  it('skips removed event when marking as reviewed', async () => {
    render(<SUT />);
    await removeEvent(initialEventIds[1]);

    const list = await screen.findByRole('list');

    expect(within(list).queryByText(eventMessage(1))).not.toBeInTheDocument();
    await within(list).findByText(eventMessage(0));
    await within(list).findByText(eventMessage(2));
  });

  it('bulk actions get current list of events', async () => {
    render(<SUT />);
    await removeEvent(initialEventIds[1]);

    await userEvent.click(await screen.findByRole('button', { name: /bulk actions/i }));
    await userEvent.click(await screen.findByRole('menuitem', { name: /test action/i }));

    await waitFor(() => {
      expect(bulkAction).toHaveBeenCalledWith([eventByIndex(0), eventByIndex(2)]);
    });
  });

  it('shows review progress counter', async () => {
    render(<SUT />);
    await screen.findByText(/0\/3/);

    await markEventAsReviewedFromList(initialEventIds[0]);
    await screen.findByText(/1\/3/);

    await markEventAsReviewedFromList(initialEventIds[1]);
    await screen.findByText(/2\/3/);
  });
});
