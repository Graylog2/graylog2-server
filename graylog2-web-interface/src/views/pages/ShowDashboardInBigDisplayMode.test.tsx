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
import { render, waitFor } from 'wrappedTestingLibrary';

import View from 'views/logic/views/View';
import OriginalShowDashboardInBigDisplayMode from 'views/pages/ShowDashboardInBigDisplayMode';
import TestStoreProvider from 'views/test/TestStoreProvider';
import useViewsPlugin from 'views/test/testViewsPlugin';
import { createSearch } from 'fixtures/searches';
import { asMock } from 'helpers/mocking';
import useAutoRefresh from 'views/hooks/useAutoRefresh';
import useQuery from 'routing/useQuery';

const mockView = createSearch({ queryId: 'somequery' }).toBuilder()
  .type(View.Type.Dashboard)
  .id('view-id')
  .title('view title')
  .build();

jest.mock('views/pages/ShowViewPage', () => ({ children }: React.PropsWithChildren<{}>) => children);
jest.mock('routing/withLocation', () => (x) => x);
jest.mock('routing/withParams', () => (x) => x);
jest.mock('views/hooks/useAutoRefresh');
jest.mock('routing/useQuery');

const ShowDashboardInBigDisplayMode = () => (
  <TestStoreProvider view={mockView} initialQuery="somequery">
    <OriginalShowDashboardInBigDisplayMode />
  </TestStoreProvider>
);

describe('ShowDashboardInBigDisplayMode', () => {
  const autoRefreshContextValue = {
    refreshConfig: null,
    startAutoRefresh: () => {},
    stopAutoRefresh: () => {},
    restartAutoRefresh: () => {},
    animationId: 'animation-id',
  };

  useViewsPlugin();

  beforeAll(() => {
    asMock(useQuery).mockReturnValue({ interval: '30', refresh: '10' });
  });

  beforeEach(() => {
    jest.clearAllMocks();

    asMock(useAutoRefresh).mockReturnValue(autoRefreshContextValue);
  });

  it('set refresh interval correctly based on location query', async () => {
    const startAutoRefresh = jest.fn();

    asMock(useAutoRefresh).mockReturnValue({
      ...autoRefreshContextValue,
      startAutoRefresh,
    });

    render(<ShowDashboardInBigDisplayMode />);

    await waitFor(() => expect(startAutoRefresh).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(startAutoRefresh).toHaveBeenCalledWith(10000));
  });

  it('enable refresh actions', async () => {
    const startAutoRefresh = jest.fn();

    asMock(useAutoRefresh).mockReturnValue({
      ...autoRefreshContextValue,
      startAutoRefresh,
    });

    render(<ShowDashboardInBigDisplayMode />);

    await waitFor(() => expect(startAutoRefresh).toHaveBeenCalledTimes(1));
  });

  it('set new refresh interval when location query refresh param changes', async () => {
    const startAutoRefresh = jest.fn();

    asMock(useAutoRefresh).mockReturnValue({
      ...autoRefreshContextValue,
      startAutoRefresh,
    });

    const { rerender } = render(<ShowDashboardInBigDisplayMode />);

    asMock(useQuery).mockReturnValue({ interval: '30', refresh: '20' });

    rerender(<ShowDashboardInBigDisplayMode />);

    await waitFor(() => expect(startAutoRefresh).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(startAutoRefresh).toHaveBeenCalledWith(20000));
  });

  it('not change auto refresh when query refresh param did not changed', async () => {
    const startAutoRefresh = jest.fn();

    asMock(useAutoRefresh).mockReturnValue({
      ...autoRefreshContextValue,
      startAutoRefresh,
    });

    const { rerender } = render(<ShowDashboardInBigDisplayMode />);

    rerender(<ShowDashboardInBigDisplayMode />);

    await waitFor(() => expect(startAutoRefresh).toHaveBeenCalledTimes(1));
  });

  it('disable refresh actions on unmount', async () => {
    const startAutoRefresh = jest.fn();

    asMock(useAutoRefresh).mockReturnValue({
      ...autoRefreshContextValue,
      startAutoRefresh,
    });

    const { unmount } = render(<ShowDashboardInBigDisplayMode />);

    unmount();
    await waitFor(() => expect(startAutoRefresh).toHaveBeenCalledTimes(1));
  });

  it('should display view title', async () => {
    const { findByText } = render(<ShowDashboardInBigDisplayMode />);

    await findByText('view title');
  });
});
