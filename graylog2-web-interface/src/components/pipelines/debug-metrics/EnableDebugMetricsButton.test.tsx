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
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import asMock from 'helpers/mocking/AsMock';
import usePermissions from 'hooks/usePermissions';

import EnableDebugMetricsButton from './EnableDebugMetricsButton';
import useDebugMetricsConfig from './useDebugMetricsConfig';

jest.mock('./useDebugMetricsConfig');
jest.mock('hooks/usePermissions');
jest.mock('components/rules/RuleMetricsConfigContainer', () => () => <div>Rule metrics configuration</div>);

const mockConfig = (overrides: Partial<ReturnType<typeof useDebugMetricsConfig>> = {}) => {
  asMock(useDebugMetricsConfig).mockReturnValue({
    metricsEnabled: false,
    isLoading: false,
    refresh: jest.fn(),
    disable: jest.fn(),
    ...overrides,
  });
};

describe('EnableDebugMetricsButton', () => {
  beforeEach(() => {
    asMock(usePermissions).mockReturnValue({ isPermitted: () => true, isAnyPermitted: () => true });
    mockConfig();
  });

  it('renders the enable button when debug metrics are disabled', () => {
    render(<EnableDebugMetricsButton />);

    expect(screen.getByRole('button', { name: /enable debug metrics/i })).toBeInTheDocument();
  });

  it('shows the pipeline load help text on hover', async () => {
    render(<EnableDebugMetricsButton />);

    await userEvent.hover(screen.getByRole('button', { name: /enable debug metrics/i }));

    expect(await screen.findByText(/Pipeline Load \(15m\) shows the relative share/i)).toBeInTheDocument();
  });

  it('opens the rule metrics configuration modal on click', async () => {
    render(<EnableDebugMetricsButton />);

    await userEvent.click(screen.getByRole('button', { name: /enable debug metrics/i }));

    expect(screen.getByText('Rule metrics configuration')).toBeInTheDocument();
  });

  it('renders nothing when debug metrics are enabled', () => {
    mockConfig({ metricsEnabled: true });
    render(<EnableDebugMetricsButton />);

    expect(screen.queryByRole('button', { name: /enable debug metrics/i })).not.toBeInTheDocument();
  });

  it('renders nothing while loading', () => {
    mockConfig({ isLoading: true });
    render(<EnableDebugMetricsButton />);

    expect(screen.queryByRole('button', { name: /enable debug metrics/i })).not.toBeInTheDocument();
  });

  it('renders nothing without the pipeline:edit permission', () => {
    asMock(usePermissions).mockReturnValue({ isPermitted: () => false, isAnyPermitted: () => false });
    render(<EnableDebugMetricsButton />);

    expect(screen.queryByRole('button', { name: /enable debug metrics/i })).not.toBeInTheDocument();
  });
});
