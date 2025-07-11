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
import { Formik, Form } from 'formik';
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import useCurrentUser from 'hooks/useCurrentUser';
import { adminUser } from 'fixtures/users';
import asMock from 'helpers/mocking/AsMock';

import MoveRange from './MoveRange';

jest.mock('hooks/useCurrentUser');

describe('MoveRange', () => {
  type SUTProps = Partial<
    React.ComponentProps<typeof MoveRange> & {
      onSubmit?: () => void;
    }
  >;

  const SUT = ({
    onSubmit = () => {},
    effectiveTimerange = undefined,
    displayMoveRangeButtons = true,
    setCurrentTimeRange = () => {},
    initialTimerange = undefined,
    currentTimerange = undefined,
    children,
  }: SUTProps) => (
    <Formik initialValues={{}} onSubmit={onSubmit}>
      <Form>
        <MoveRange
          setCurrentTimeRange={setCurrentTimeRange}
          effectiveTimerange={effectiveTimerange}
          initialTimerange={initialTimerange}
          initialTimerangeFormat="internalIndexer"
          currentTimerange={currentTimerange}
          displayMoveRangeButtons={displayMoveRangeButtons}>
          {children}
        </MoveRange>
      </Form>
    </Formik>
  );

  const timeRange = {
    from: '2025-06-27T07:18:06.716Z',
    to: '2025-06-27T07:19:41.352Z',
    type: 'absolute' as const,
  };

  beforeEach(() => {
    asMock(useCurrentUser).mockReturnValue(adminUser);
  });

  it('should only render children, when buttons should not be displayed', async () => {
    render(<SUT displayMoveRangeButtons={false}>Example children</SUT>);

    await screen.findByText('Example children');

    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });

  it('should select previous time range', async () => {
    const onSubmit = jest.fn();
    const setCurrentTimeRange = jest.fn();

    render(
      <SUT
        onSubmit={onSubmit}
        setCurrentTimeRange={setCurrentTimeRange}
        effectiveTimerange={timeRange}
        initialTimerange={timeRange}
        currentTimerange={timeRange}
      />,
    );

    const showPrevButton = await screen.findByRole('button', { name: /show previous 1 minute 35 seconds/i });
    await userEvent.click(showPrevButton);

    const newTimeRange = {
      from: '2025-06-27 09:16:32.080',
      to: '2025-06-27 09:18:06.716',
      type: 'absolute',
    };

    expect(setCurrentTimeRange).toHaveBeenCalledWith(newTimeRange);

    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1));
  });

  it('should select next time range', async () => {
    const onSubmit = jest.fn();
    const setCurrentTimeRange = jest.fn();

    render(
      <SUT
        onSubmit={onSubmit}
        setCurrentTimeRange={setCurrentTimeRange}
        effectiveTimerange={timeRange}
        initialTimerange={timeRange}
        currentTimerange={timeRange}
      />,
    );

    const showPrevButton = await screen.findByRole('button', { name: /show next 1 minute 35 seconds/i });
    await userEvent.click(showPrevButton);

    const newTimeRange = {
      from: '2025-06-27 09:19:41.352',
      to: '2025-06-27 09:21:15.988',
      type: 'absolute',
    };

    expect(setCurrentTimeRange).toHaveBeenCalledWith(newTimeRange);

    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1));
  });

  it('should calculate correct time range, when user has time range (not UTC)', async () => {
    asMock(useCurrentUser).mockReturnValue(adminUser.toBuilder().timezone('America/Los_Angeles').build());

    const onSubmit = jest.fn();
    const setCurrentTimeRange = jest.fn();

    render(
      <SUT
        onSubmit={onSubmit}
        setCurrentTimeRange={setCurrentTimeRange}
        effectiveTimerange={timeRange}
        initialTimerange={timeRange}
        currentTimerange={timeRange}
      />,
    );

    const showPrevButton = await screen.findByRole('button', { name: /show next 1 minute 35 seconds/i });
    await userEvent.click(showPrevButton);

    const newTimeRange = {
      from: '2025-06-27 09:19:41.352',
      to: '2025-06-27 09:21:15.988',
      type: 'absolute',
    };

    expect(setCurrentTimeRange).toHaveBeenCalledWith(newTimeRange);

    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1));
  });
});
