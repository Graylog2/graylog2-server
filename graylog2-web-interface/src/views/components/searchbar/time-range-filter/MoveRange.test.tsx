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
    initialTimeRange = undefined,
    currentTimeRange = undefined,
    children,
  }: SUTProps) => (
    <Formik initialValues={{}} onSubmit={onSubmit}>
      <Form>
        <MoveRange
          setCurrentTimeRange={setCurrentTimeRange}
          effectiveTimerange={effectiveTimerange}
          initialTimeRange={initialTimeRange}
          initialTimeFormat="internalIndexer"
          currentTimeRange={currentTimeRange}
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
        initialTimeRange={timeRange}
        currentTimeRange={timeRange}
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
        initialTimeRange={timeRange}
        currentTimeRange={timeRange}
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
        initialTimeRange={timeRange}
        currentTimeRange={timeRange}
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
