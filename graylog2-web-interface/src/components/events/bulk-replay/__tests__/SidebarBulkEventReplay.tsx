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
/* eslint-disable jest/expect-expect */

import * as React from 'react';
import { render, screen, waitFor, within } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import { PluginManifest } from 'graylog-web-plugin/plugin';

import { usePlugin } from 'views/test/testPlugins';
import MenuItem from 'components/bootstrap/menuitem/MenuItem';
import SidebarBulkEventReplay from 'components/events/bulk-replay/SidebarBulkEventReplay';
import EventReplaySelectedProvider from 'contexts/EventReplaySelectedProvider';
import asMock from 'helpers/mocking/AsMock';
import useEventsById from 'components/events/bulk-replay/hooks/useEventsById';
import useSessionInitialEventIds from 'components/events/bulk-replay/hooks/useSessionInitialEventIds';
import StringUtils from 'util/StringUtils';

import events from './events.fixtures';

const initialEventIds = ['01JH007XPDF710TPJZT8K2CN3W', '01JH006340WP7HQ5E7P71Q9HHX', '01JH0029TS9PX5ED87TZ1RVRT2'];

jest.mock('components/events/bulk-replay/hooks/useEventsById');
jest.mock('components/events/bulk-replay/hooks/useSessionInitialEventIds');
jest.mock('components/events/ReplaySearch', () => ({ alertId }: { alertId: string }) => (
  <span>Replaying search for event {alertId}</span>
));

const markEventAsReviewedFromList = async (eventId: string) => {
  const list = await screen.findByRole('list');

  const markAsReviewedButton = await within(list).findByRole('button', {
    name: new RegExp(`mark event "${eventId}" as reviewed`, 'i'),
  });

  return userEvent.click(markAsReviewedButton);
};

const findDropdownButton = () => screen.findByTitle(/show selected events/i);

const openDropdown = async () => {
  const openButton = await findDropdownButton();

  return userEvent.click(openButton);
};

const expectReplayingEvent = async (eventId: string) => {
  const DropdownButton = await findDropdownButton();

  const message = StringUtils.escapeRegExp(events?.[eventId]?.event?.message);

  return within(DropdownButton).findByText(new RegExp(message, 'i'));
};

const removeEvent = async (eventId: string) => {
  const removeEventButton = await screen.findByRole('button', {
    name: new RegExp(`remove event "${eventId}" from list`, 'i'),
  });

  return userEvent.click(removeEventButton);
};

const eventByIndex = (index: number) => events[initialEventIds[index]].event;
const eventMessage = (index: number) => eventByIndex(index).message;

const onClose = jest.fn();

const markEventAsReviewedFromHeader = async (eventId: string) => {
  const dropdownButton = await findDropdownButton();

  const markAsReviewedButton = await within(dropdownButton).findByRole('button', {
    name: new RegExp(`mark event "${eventId}" as reviewed`, 'i'),
  });

  return userEvent.click(markAsReviewedButton);
};

const SUT = () => (
  <EventReplaySelectedProvider initialEventIds={initialEventIds}>
    <SidebarBulkEventReplay onClose={onClose} />
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
    asMock(useEventsById).mockReturnValue({ data: events });
    asMock(useSessionInitialEventIds).mockReturnValue(initialEventIds);
  });

  it('renders list of selected events', async () => {
    render(<SUT />);
    await openDropdown();
    const list = await screen.findByRole('list');
    await within(list).findByText(eventMessage(0));
    await within(list).findByText(eventMessage(1));
    await within(list).findByText(eventMessage(2));

    await expectReplayingEvent(initialEventIds[0]);
  });

  it('clicking delete button removes event from list', async () => {
    render(<SUT />);
    await openDropdown();
    const list = await screen.findByRole('list');
    await within(list).findByText(eventMessage(0));
    await removeEvent(initialEventIds[0]);
    await within(list).findByText(eventMessage(1));
    await expectReplayingEvent(initialEventIds[1]);

    expect(within(list).queryByText(eventMessage(0))).not.toBeInTheDocument();
  });

  it('marking events as reviewed jumps to next one', async () => {
    render(<SUT />);
    await openDropdown();
    await markEventAsReviewedFromList(initialEventIds[0]);

    await expectReplayingEvent(initialEventIds[1]);

    await markEventAsReviewedFromList(initialEventIds[1]);
    await expectReplayingEvent(initialEventIds[2]);

    await markEventAsReviewedFromList(initialEventIds[2]);
    await screen.findByText(
      'You are done reviewing all events. You can now select a bulk action to apply to all remaining events.',
    );
  });

  it('allows jumping to specific events', async () => {
    render(<SUT />);
    await expectReplayingEvent(initialEventIds[0]);

    await openDropdown();

    await userEvent.click(await screen.findByText(eventMessage(1)));

    await expectReplayingEvent(initialEventIds[1]);

    await userEvent.click(await screen.findByText(eventMessage(0)));

    await expectReplayingEvent(initialEventIds[0]);
  });

  it('skips removed event when jumping to next one', async () => {
    render(<SUT />);
    await openDropdown();
    await removeEvent(initialEventIds[1]);
    await markEventAsReviewedFromList(initialEventIds[0]);
    await expectReplayingEvent(initialEventIds[2]);
  });

  it('skips event marked as reviewed when jumping to next one', async () => {
    render(<SUT />);
    await openDropdown();
    await markEventAsReviewedFromList(initialEventIds[1]);
    await markEventAsReviewedFromList(initialEventIds[0]);
    await expectReplayingEvent(initialEventIds[2]);
  });

  it('bulk actions get current list of events', async () => {
    render(<SUT />);
    await openDropdown();
    await removeEvent(initialEventIds[1]);

    await userEvent.click(await screen.findByRole('button', { name: /bulk actions/i }));
    await userEvent.click(await screen.findByRole('menuitem', { name: /test action/i }));

    await waitFor(() => {
      expect(bulkAction).toHaveBeenCalledWith([eventByIndex(0), eventByIndex(2)]);
    });
  });

  it('navigation buttons switch events', async () => {
    render(<SUT />);
    await expectReplayingEvent(initialEventIds[0]);

    const prev = await screen.findByTitle(/previous event/i);
    const next = await screen.findByTitle(/next event/i);

    await userEvent.click(next);
    await expectReplayingEvent(initialEventIds[1]);

    await userEvent.click(prev);
    await expectReplayingEvent(initialEventIds[0]);
  });

  it('marking events as reviewed from the header and jumps to next one', async () => {
    render(<SUT />);
    await openDropdown();
    await markEventAsReviewedFromHeader(initialEventIds[0]);

    await expectReplayingEvent(initialEventIds[1]);

    await markEventAsReviewedFromHeader(initialEventIds[1]);
    await expectReplayingEvent(initialEventIds[2]);

    await markEventAsReviewedFromHeader(initialEventIds[2]);
    await screen.findByText(
      'You are done reviewing all events. You can now select a bulk action to apply to all remaining events.',
    );
  });
});
