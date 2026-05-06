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

import { RulesActions, RulesStore } from 'stores/rules/RulesStore';

import DebugMetricsBanner from './DebugMetricsBanner';

jest.mock('stores/rules/RulesStore', () => ({
  RulesStore: {
    listen: jest.fn(() => () => {}),
    getInitialState: jest.fn(() => ({ metricsConfig: { metrics_enabled: false } })),
  },
  RulesActions: {
    loadMetricsConfig: jest.fn(() => Promise.resolve()),
    updateMetricsConfig: jest.fn(() => Promise.resolve()),
  },
}));

jest.mock('components/rules/RuleMetricsConfigContainer', () => {
  const ReactActual = jest.requireActual('react');

  return {
    __esModule: true,
    default: () => ReactActual.createElement('div', { 'data-testid': 'rule-metrics-config-modal' }),
  };
});

const mockedLoadMetricsConfig = RulesActions.loadMetricsConfig as unknown as jest.Mock;
const mockedUpdateMetricsConfig = RulesActions.updateMetricsConfig as unknown as jest.Mock;
const mockedGetInitialState = (RulesStore as unknown as { getInitialState: jest.Mock }).getInitialState;

const setMetricsConfig = (metricsConfig: { metrics_enabled: boolean } | undefined) => {
  mockedGetInitialState.mockReturnValue({ metricsConfig });
};

describe('DebugMetricsBanner', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    setMetricsConfig({ metrics_enabled: false });
  });

  it('renders the disabled banner with an enable action when metrics are off', async () => {
    render(<DebugMetricsBanner />);

    expect(await screen.findByText(/Pipeline Load is unavailable/i)).toBeInTheDocument();
    expect(screen.getByText(/Enable debug metrics to start collecting timer data/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Enable debug metrics/i })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Disable debug metrics/i })).not.toBeInTheDocument();
  });

  it('opens the metrics config modal when the enable action is clicked', async () => {
    render(<DebugMetricsBanner />);

    await userEvent.click(await screen.findByRole('button', { name: /Enable debug metrics/i }));

    expect(await screen.findByTestId('rule-metrics-config-modal')).toBeInTheDocument();
  });

  it('renders the enabled banner with a disable action when metrics are on', async () => {
    setMetricsConfig({ metrics_enabled: true });
    render(<DebugMetricsBanner />);

    expect(await screen.findByText(/Debug metrics are enabled/i)).toBeInTheDocument();
    expect(screen.getByText(/Disable them when you are finished troubleshooting/i)).toBeInTheDocument();
    expect(screen.getByText(/15-minute window/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Disable debug metrics/i })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Enable debug metrics/i })).not.toBeInTheDocument();
  });

  it('disables debug metrics when the disable action is clicked', async () => {
    setMetricsConfig({ metrics_enabled: true });
    render(<DebugMetricsBanner />);

    await userEvent.click(await screen.findByRole('button', { name: /Disable debug metrics/i }));

    expect(mockedUpdateMetricsConfig).toHaveBeenCalledWith({ metrics_enabled: false });
  });

  it('renders nothing while config is loading', () => {
    setMetricsConfig(undefined);
    render(<DebugMetricsBanner />);

    expect(screen.queryByText(/Pipeline Load is unavailable/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/Debug metrics are enabled/i)).not.toBeInTheDocument();
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });

  it('triggers a config load on mount', () => {
    render(<DebugMetricsBanner />);

    expect(mockedLoadMetricsConfig).toHaveBeenCalled();
  });
});
