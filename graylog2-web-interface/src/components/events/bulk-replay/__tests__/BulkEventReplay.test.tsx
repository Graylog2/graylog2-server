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
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import { PluginManifest } from 'graylog-web-plugin/plugin';

import { usePlugin } from 'views/test/testPlugins';
import MenuItem from 'components/bootstrap/menuitem/MenuItem';

import events from './events.fixtures';

import BulkEventReplay from '../BulkEventReplay';

const initialEventIds = [
  '01JH007XPDF710TPJZT8K2CN3W',
  '01JH006340WP7HQ5E7P71Q9HHX',
  '01JH0029TS9PX5ED87TZ1RVRT2',
];

jest.mock('components/events/ReplaySearch', () => ({ alertId }: { alertId: string }) => <span>Replaying search for event {alertId}</span>);

const markEventAsInvestigated = async (eventId: string) => {
  const markAsInvestigatedButton = await screen.findByRole('button', { name: new RegExp(`mark event "${eventId}" as investigated`, 'i') });

  return userEvent.click(markAsInvestigatedButton);
};

const removeEvent = async (eventId: string) => {
  const removeEventButton = await screen.findByRole('button', { name: new RegExp(`remove event "${eventId}" from list`, 'i') });

  return userEvent.click(removeEventButton);
};

const expectReplayingEvent = (eventId: string) => screen.findByText(new RegExp(`replaying search for event ${eventId}`, 'i'));

const eventByIndex = (index: number) => events[initialEventIds[index]].event;
const eventMessage = (index: number) => eventByIndex(index).message;

const SUT = (props: Partial<React.ComponentProps<typeof BulkEventReplay>>) => (
  <BulkEventReplay events={events} initialEventIds={initialEventIds} onClose={() => {}} {...props} />
);

const bulkAction = jest.fn();
const testPlugin = new PluginManifest({}, {
  'views.components.eventActions': [{
    key: 'test-bulk-action',
    component: ({ events: _events }) => (
      <MenuItem onClick={() => bulkAction(_events)}>Test Action</MenuItem>
    ),
    useCondition: () => true,
    isBulk: true,
  }],
});

describe('BulkEventReplay', () => {
  usePlugin(testPlugin);

  it('calls `onClose` when close button is clicked', async () => {
    const onClose = jest.fn();
    render(<SUT onClose={onClose} />);
    const closeButton = await screen.findByRole('button', { name: 'Close' });
    userEvent.click(closeButton);

    await waitFor(() => {
      expect(onClose).toHaveBeenCalled();
    });
  });

  it('renders list of selected events', async () => {
    render(<SUT />);
    await screen.findByText(eventMessage(0));
    await screen.findByText(eventMessage(1));
    await screen.findByText(eventMessage(2));

    await expectReplayingEvent(initialEventIds[0]);
  });

  it('clicking delete button removes event from list', async () => {
    render(<SUT />);
    await removeEvent(initialEventIds[0]);

    await screen.findByText(eventMessage(1));
    await expectReplayingEvent(initialEventIds[1]);

    expect(screen.queryByText(eventMessage(0))).not.toBeInTheDocument();
  });

  it('marking events as investigated jumps to next one', async () => {
    render(<SUT />);
    await markEventAsInvestigated(initialEventIds[0]);

    await expectReplayingEvent(initialEventIds[1]);

    await markEventAsInvestigated(initialEventIds[1]);
    await expectReplayingEvent(initialEventIds[2]);

    await markEventAsInvestigated(initialEventIds[2]);
    await screen.findByText('You are done investigating all events. You can now select a bulk action to apply to all remaining events, or close the page to return to the events list.');
  });

  it('allows jumping to specific events', async () => {
    render(<SUT />);
    await expectReplayingEvent(initialEventIds[0]);

    userEvent.click(await screen.findByText(eventMessage(1)));

    await expectReplayingEvent(initialEventIds[1]);

    userEvent.click(await screen.findByText(eventMessage(0)));

    await expectReplayingEvent(initialEventIds[0]);
  });

  it('skips removed event when jumping to next one', async () => {
    render(<SUT />);
    await removeEvent(initialEventIds[1]);
    await markEventAsInvestigated(initialEventIds[0]);
    await expectReplayingEvent(initialEventIds[2]);
  });

  it('skips event marked as investigated when jumping to next one', async () => {
    render(<SUT />);
    await markEventAsInvestigated(initialEventIds[1]);
    await markEventAsInvestigated(initialEventIds[0]);
    await expectReplayingEvent(initialEventIds[2]);
  });

  it('bulk actions get current list of events', async () => {
    render(<SUT />);
    await removeEvent(initialEventIds[1]);

    userEvent.click(await screen.findByRole('button', { name: /bulk actions/i }));
    userEvent.click(await screen.findByRole('menuitem', { name: /test action/i }));

    await waitFor(() => {
      expect(bulkAction).toHaveBeenCalledWith([eventByIndex(0), eventByIndex(2)]);
    });
  });
});
