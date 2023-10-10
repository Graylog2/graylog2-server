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
import { fireEvent, render, screen } from 'wrappedTestingLibrary';
import 'helpers/mocking/react-dom_mock';

import { Formik } from 'formik';

import { RefreshActions } from 'views/stores/RefreshStore';
import { asMock } from 'helpers/mocking';
import useRefreshConfig from 'views/components/searchbar/useRefreshConfig';
import useSearchConfiguration from 'hooks/useSearchConfiguration';
import type { SearchesConfig } from 'components/search/SearchConfig';

import RefreshControls from './RefreshControls';

jest.useFakeTimers();

jest.mock('./useRefreshConfig');

jest.mock('views/stores/RefreshStore', () => ({
  RefreshActions: {
    enable: jest.fn(),
    disable: jest.fn(),
  },
  RefreshStore: {},
}));

jest.mock('hooks/useSearchConfiguration');

const autoRefreshOptions = {
  PT1S: '1 second',
  PT2S: '2 second',
  PT5S: '5 second',
  PT1M: '1 minute',
  PT5M: '5 minutes',
};

describe('RefreshControls', () => {
  const SUT = ({ onSubmit }: { onSubmit?: () => void }) => (
    <Formik initialValues={{}} onSubmit={onSubmit}>
      <RefreshControls />
    </Formik>
  );

  SUT.defaultProps = {
    onSubmit: () => {},
  };

  beforeEach(() => {
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
      asMock(useRefreshConfig).mockReturnValue({ enabled, interval });
      render(<SUT />);

      await screen.findByLabelText(/refresh search controls/i);
    });
  });

  it('should start the interval', async () => {
    asMock(useRefreshConfig).mockReturnValue({ enabled: false, interval: 1000 });
    render(<SUT />);

    fireEvent.click(await screen.findByTitle(/start refresh/i));

    expect(RefreshActions.enable).toHaveBeenCalled();
  });

  it('should stop the interval', async () => {
    asMock(useRefreshConfig).mockReturnValue({ enabled: true, interval: 1000 });
    render(<SUT />);

    fireEvent.click(await screen.findByTitle(/pause refresh/i));

    expect(RefreshActions.disable).toHaveBeenCalled();
  });
});
