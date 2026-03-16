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
import userEvent from '@testing-library/user-event';
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import type { Location } from 'history';

import type { SearchParams } from 'stores/PaginationTypes';
import TableFetchContext, { type ContextValue } from 'components/common/PaginatedEntityTable/TableFetchContext';
import { asMock } from 'helpers/mocking';
import useSearchConfiguration from 'hooks/useSearchConfiguration';
import type { SearchesConfig } from 'components/search/SearchConfig';
import useLocation from 'routing/useLocation';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import type { AutoRefreshContextType } from 'views/components/contexts/AutoRefreshContext';
import useAutoRefresh from 'views/hooks/useAutoRefresh';
import useMinimumRefreshInterval from 'views/hooks/useMinimumRefreshInterval';

import EventsRefreshControls from './EventsRefreshControls';

jest.mock('hooks/useSearchConfiguration');
jest.mock('routing/useLocation');
jest.mock('logic/telemetry/useSendTelemetry');
jest.mock('views/hooks/useAutoRefresh');
jest.mock('views/hooks/useMinimumRefreshInterval');

const autoRefreshOptions: SearchesConfig['auto_refresh_timerange_options'] = {
  PT1S: '1 second',
  PT2S: '2 seconds',
  PT5S: '5 seconds',
};

describe('EventsRefreshControls', () => {
  const sendTelemetry = jest.fn();

  const autoRefreshContextValue: AutoRefreshContextType = {
    refreshConfig: null,
    stopAutoRefresh: jest.fn(),
    startAutoRefresh: jest.fn(),
    restartAutoRefresh: jest.fn(),
    animationId: 'animation-id',
  };

  const renderSUT = (searchParamsOverrides: Partial<SearchParams> = {}) => {
    const contextValue: ContextValue = {
      searchParams: {
        page: 1,
        pageSize: 10,
        query: '',
        sort: { attributeId: 'timestamp', direction: 'desc' },
        sliceCol: undefined,
        slice: undefined,
        filters: undefined,
        ...searchParamsOverrides,
      },
      refetch: jest.fn(),
      attributes: [],
      entityTableId: 'events-table',
    };

    return render(
      <TableFetchContext.Provider value={contextValue}>
        <EventsRefreshControls />
      </TableFetchContext.Provider>,
    );
  };

  beforeEach(() => {
    jest.clearAllMocks();

    asMock(useLocation).mockReturnValue({ pathname: '/alerts' } as Location);
    asMock(useSendTelemetry).mockReturnValue(sendTelemetry);

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

  it('renders enabled refresh controls when slicing is inactive', async () => {
    renderSUT();

    expect(await screen.findByTitle(/start refresh/i)).toBeEnabled();
    expect(screen.getByRole('button', { name: /not updating/i })).toBeEnabled();
    expect(screen.queryByRole('button', { name: /auto refresh unavailable/i })).not.toBeInTheDocument();
  });

  it('disables refresh controls and shows help while slicing is active', async () => {
    const user = userEvent.setup();

    renderSUT({ sliceCol: 'source' });

    expect(await screen.findByTitle(/start refresh/i)).toBeDisabled();
    expect(screen.getByRole('button', { name: /not updating/i })).toBeDisabled();

    await user.hover(screen.getByRole('button', { name: /auto refresh unavailable/i }));

    expect(await screen.findByText(/auto refresh is turned off during slicing to avoid performance issues/i)).toBeInTheDocument();
  });

  it('stops auto refresh when slicing becomes active and refresh is enabled', async () => {
    const stopAutoRefresh = jest.fn();

    asMock(useAutoRefresh).mockReturnValue({
      ...autoRefreshContextValue,
      stopAutoRefresh,
      refreshConfig: { enabled: true, interval: 1000 },
    });

    renderSUT({ sliceCol: 'source' });

    await waitFor(() => expect(stopAutoRefresh).toHaveBeenCalledTimes(1));
  });

  it('does not stop auto refresh when slicing is active but refresh is already disabled', () => {
    const stopAutoRefresh = jest.fn();

    asMock(useAutoRefresh).mockReturnValue({
      ...autoRefreshContextValue,
      stopAutoRefresh,
      refreshConfig: { enabled: false, interval: 1000 },
    });

    renderSUT({ sliceCol: 'source' });

    expect(stopAutoRefresh).not.toHaveBeenCalled();
  });

  it('starts auto refresh when slicing is inactive', async () => {
    const user = userEvent.setup();
    const startAutoRefresh = jest.fn();

    asMock(useAutoRefresh).mockReturnValue({
      ...autoRefreshContextValue,
      startAutoRefresh,
      refreshConfig: { enabled: false, interval: 1000 },
    });

    renderSUT();

    await user.click(await screen.findByTitle(/start refresh/i));

    expect(startAutoRefresh).toHaveBeenCalledWith(1000);
  });
});
