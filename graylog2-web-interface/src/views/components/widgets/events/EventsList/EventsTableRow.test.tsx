import React from 'react';
import { render, screen } from 'wrappedTestingLibrary';
import * as Immutable from 'immutable';
import userEvent from '@testing-library/user-event';

import EventsTableRow from './EventsTableRow';

const event = {
  id: 'ievent-id-1',
  name: 'Event 1',
  status: null,
  assigned_to: null,
  created_at: '2024-02-26T15:32:24.666Z',
  updated_at: '2024-02-26T15:32:24.666Z',
  priority: null,
  archived: false,
  replay_info: null,
};

describe('EventsList', () => {
  it('should render list of events', async () => {
    const mockViewEvent = jest.fn();

    render(
      <table>
        <tbody>
          <EventsTableRow event={event}
                          fields={Immutable.OrderedSet(['name'])} />
        </tbody>
      </table>,
    );

    userEvent.click(await screen.findByRole('button', { name: /Event 1/i }));

    expect(mockViewEvent).toHaveBeenCalledWith('event-id-1');
  });
});
