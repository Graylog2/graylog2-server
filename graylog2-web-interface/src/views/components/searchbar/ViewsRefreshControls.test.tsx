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
import React from 'react';
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import { Form, Formik, useFormikContext } from 'formik';
import userEvent from '@testing-library/user-event';

import { asMock } from 'helpers/mocking';
import useSearchConfiguration from 'hooks/useSearchConfiguration';
import type { SearchesConfig } from 'components/search/SearchConfig';
import Button from 'preflight/components/common/Button';
import useAutoRefresh from 'views/hooks/useAutoRefresh';
import useMinimumRefreshInterval from 'views/hooks/useMinimumRefreshInterval';
import useViewsPlugin from 'views/test/testViewsPlugin';
import TestStoreProvider from 'views/test/TestStoreProvider';

import ViewsRefreshControls from './ViewsRefreshControls';

jest.useFakeTimers();

jest.mock('hooks/useSearchConfiguration');
jest.mock('views/hooks/useAutoRefresh');
jest.mock('views/hooks/useMinimumRefreshInterval');

const autoRefreshOptions = {
  PT1S: '1 second',
  PT2S: '2 second',
  PT5S: '5 second',
  PT1M: '1 minute',
  PT5M: '5 minutes',
};

describe('RefreshControls', () => {
  useViewsPlugin();

  const SUT = ({ onSubmit = () => {}, children }: { onSubmit?: () => void, children?: React.ReactNode }) => (
    <TestStoreProvider>
      <Formik initialValues={{}} onSubmit={onSubmit}>
        <Form>
          <ViewsRefreshControls disable={false} />
          {children}
        </Form>
      </Formik>
    </TestStoreProvider>
  );

  const TriggerFormChangeButton = () => {
    const { setFieldValue, values } = useFormikContext();

    return (
      <>
        Current value is: {values['example-field']}
        <Button onClick={() => setFieldValue('example-field', 'example-value')}>
          Change form field value
        </Button>
      </>
    );
  };

  const autoRefreshContextValue = {
    refreshConfig: null,
    stopAutoRefresh: () => {},
    startAutoRefresh: () => {},
    restartAutoRefresh: () => {},
    animationId: 'animation-id',
  };

  beforeEach(() => {
    asMock(useAutoRefresh).mockReturnValue(autoRefreshContextValue);

    asMock(useMinimumRefreshInterval).mockReturnValue({
      data: 'PT1S',
      isInitialLoading: false,
    });

    asMock(useSearchConfiguration).mockReturnValue({
      config: {
        auto_refresh_timerange_options: autoRefreshOptions,
        default_auto_refresh_option: 'PT5S',
      } as unknown as SearchesConfig,
      refresh: jest.fn(),
    });
  });

  describe('rendering', () => {
    it.each`
    enabled      | interval
    ${true}      | ${1000}
    ${true}      | ${2000}
    ${true}      | ${5000}
    ${true}      | ${10000}
    ${true}      | ${30000}
    ${true}      | ${60000}
    ${true}      | ${300000}
    ${false}     | ${300000}
    ${false}     | ${1000}
  `('renders refresh controls with enabled: $enabled and interval: $interval', async ({ enabled, interval }) => {
      asMock(useAutoRefresh).mockReturnValue({
        ...autoRefreshContextValue,
        refreshConfig: { enabled, interval },
      });

      render(<SUT />);

      await screen.findByLabelText(/refresh search controls/i);
    });
  });

  it('should start the interval', async () => {
    const startAutoRefresh = jest.fn();

    asMock(useAutoRefresh).mockReturnValue({
      ...autoRefreshContextValue,
      startAutoRefresh,
      refreshConfig: { enabled: false, interval: 1000 },
    });

    render(<SUT />);

    userEvent.click(await screen.findByTitle(/start refresh/i));

    await waitFor(() => expect(startAutoRefresh).toHaveBeenCalledWith(1000));
  });

  it('should stop the interval', async () => {
    const stopAutoRefresh = jest.fn();

    asMock(useAutoRefresh).mockReturnValue({
      ...autoRefreshContextValue,
      stopAutoRefresh,
      refreshConfig: { enabled: true, interval: 1000 },
    });

    render(<SUT />);

    userEvent.click(await screen.findByTitle(/pause refresh/i));

    expect(stopAutoRefresh).toHaveBeenCalled();
  });

  it('should submit the form when there are changes when starting the interval', async () => {
    const onSubmitMock = jest.fn();

    render(
      <SUT onSubmit={onSubmitMock}>
        <TriggerFormChangeButton />
      </SUT>,
    );

    userEvent.click(await screen.findByRole('button', { name: /change form field value/i }));
    userEvent.click(await screen.findByTitle(/start refresh/i));

    await waitFor(() => expect(onSubmitMock).toHaveBeenCalled());
  });

  it('should stop the interval when there are form changes while the interval is active', async () => {
    const stopAutoRefresh = jest.fn();

    asMock(useAutoRefresh).mockReturnValue({
      ...autoRefreshContextValue,
      stopAutoRefresh,
      refreshConfig: { enabled: true, interval: 5000 },
    });

    render((
      <SUT>
        <TriggerFormChangeButton />
      </SUT>
    ));

    await userEvent.click(await screen.findByRole('button', { name: /change form field value/i }));

    await screen.findByText(/example-value/i);

    await waitFor(() => expect(stopAutoRefresh).toHaveBeenCalled());
  });
});
